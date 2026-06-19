package com.wanlv.app.digitalhuman

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import com.wanlv.app.repository.DigitalHumanRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import org.webrtc.VideoTrack
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DigitalHumanSessionManager(
    private val appContext: Context,
    private val repository: DigitalHumanRepository = DigitalHumanRepository()
) {
    private val eglBase: EglBase = EglBase.create()
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioDeviceModule: AudioDeviceModule by lazy { createAudioDeviceModule() }
    private val peerConnectionFactory: PeerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val stateLock = Any()
    private val observerJob = SupervisorJob()
    private val observerScope = CoroutineScope(observerJob + Dispatchers.Default)
    @Volatile private var peerConnection: PeerConnection? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var remoteAudioTrack: AudioTrack? = null
    private var videoSink: GreenScreenVideoSink? = null
    private val connectionGeneration = AtomicLong(0L)
    private var activeServerSession: ServerSession? = null
    private var connectionTimeoutJob: Job? = null
    private var disconnectedReleaseJob: Job? = null
    @Volatile private var connectedSessionId: Long? = null

    val sessionId: Long?
        get() = connectedSessionId

    suspend fun connect(
        baseUrl: String,
        onVideoFrame: (Bitmap) -> Unit,
        onConnectionLost: (String) -> Unit = {}
    ): Long {
        connectedSessionId?.let { return it }
        prepareMediaAudioOutput()
        val generation = connectionGeneration.incrementAndGet()
        val previousSession = synchronized(stateLock) {
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = null
            disconnectedReleaseJob?.cancel()
            disconnectedReleaseJob = null
            val session = activeServerSession
            activeServerSession = null
            connectedSessionId = null
            session
        }
        disconnectPeerOnly()
        closeServerSessionAsync(previousSession, "new_connection")

        val iceGatheringComplete = CompletableDeferred<Unit>()
        val connectionEstablished = CompletableDeferred<Unit>()
        val peerReference = AtomicReference<PeerConnection?>(null)
        val observer = DigitalHumanPeerObserver(
            onIceComplete = {
                if (!iceGatheringComplete.isCompleted) iceGatheringComplete.complete(Unit)
            },
            onConnectionStateChange = { state ->
                peerReference.get()?.let { peer ->
                    handleConnectionState(
                        generation = generation,
                        peer = peer,
                        state = state,
                        connectionEstablished = connectionEstablished,
                        onConnectionLost = onConnectionLost
                    )
                }
            },
            onTrack = { track ->
                peerReference.get()?.let { peer ->
                    when (track) {
                        is VideoTrack -> bindVideoTrack(generation, peer, track, onVideoFrame)
                        is AudioTrack -> bindAudioTrack(generation, peer, track)
                    }
                }
            }
        )
        val peer = createPeerConnection(observer)
        peerReference.set(peer)
        synchronized(stateLock) {
            if (connectionGeneration.get() != generation) {
                peer.close()
                peer.dispose()
                throw CancellationException("数字人连接已取消")
            }
            peerConnection = peer
        }

        var offeredSession: ServerSession? = null
        var sessionClaimed = false
        try {
            val transceiverInit = RtpTransceiver.RtpTransceiverInit(
                RtpTransceiver.RtpTransceiverDirection.RECV_ONLY
            )
            peer.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO, transceiverInit)
            peer.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, transceiverInit)

            val offer = peer.createOfferSuspend()
            ensureConnectionCurrent(generation, peer)
            peer.setLocalDescriptionSuspend(offer)
            // 重点：局域网模式不使用 STUN/TURN，但必须等 host candidate 收集完整，不能把半成品 offer 发给服务端。
            val iceComplete = withTimeoutOrNull(IceGatheringTimeoutMillis) {
                iceGatheringComplete.await()
                true
            } ?: false
            if (!iceComplete) error("Android 局域网 ICE 候选收集超时")
            ensureConnectionCurrent(generation, peer)
            val localSdp = peer.localDescription?.description ?: offer.description
            if (!localSdp.hasIceCandidate()) {
                error("Android 未生成局域网 ICE 候选，请检查网络权限与 Wi-Fi 连接")
            }
            val answer = repository.offer(baseUrl = baseUrl, sdp = localSdp)
            if (answer.sessionId == 0L) error("数字人 /offer 返回内容不完整")

            offeredSession = ServerSession(answer.sessionId, baseUrl)
            if (answer.sdp.isBlank() || !answer.sdp.hasIceCandidate()) {
                closeServerSessionAsync(offeredSession, "invalid_offer")
                offeredSession = null
                error("数字人 /offer 未返回可用的局域网 ICE 候选")
            }
            // 重点：先登记服务端会话；若 /offer 返回时用户已经退出，必须立即释放这个迟到的 session。
            val claimed = synchronized(stateLock) {
                if (connectionGeneration.get() == generation && peerConnection === peer) {
                    activeServerSession = offeredSession
                    true
                } else {
                    false
                }
            }
            if (!claimed) {
                closeServerSessionAsync(offeredSession, "stale_offer")
                offeredSession = null
                throw CancellationException("数字人连接已取消")
            }
            sessionClaimed = true

            peer.setRemoteDescriptionSuspend(
                SessionDescription(SessionDescription.Type.ANSWER, answer.sdp)
            )
            ensureConnectionCurrent(generation, peer)
            synchronized(stateLock) {
                ensureConnectionCurrent(generation, peer)
                connectedSessionId = answer.sessionId
            }
            // 重点：与 Web 端一致，远端 SDP 设置成功即结束前台 /offer 状态，真实媒体连接改由后台超时兜底。
            scheduleConnectionTimeout(
                generation = generation,
                peer = peer,
                connectionEstablished = connectionEstablished,
                onConnectionLost = onConnectionLost
            )
            return answer.sessionId
        } catch (error: Throwable) {
            val termination = invalidateAttempt(generation, peer)
            disconnectPeerOnly(peer)
            closeServerSessionAsync(
                termination.session ?: offeredSession.takeUnless { sessionClaimed },
                if (error is CancellationException) "connect_cancelled" else "connect_failed"
            )
            throw error
        }
    }

    suspend fun speak(baseUrl: String, text: String) {
        val currentSessionId = sessionId ?: return
        repository.speak(baseUrl = baseUrl, text = text, sessionId = currentSessionId)
    }

    suspend fun interrupt(baseUrl: String) {
        val currentSessionId = sessionId ?: return
        runCatching { repository.interrupt(baseUrl = baseUrl, sessionId = currentSessionId) }
    }

    fun disconnect(reason: String = "manual_disconnect") {
        connectionGeneration.incrementAndGet()
        val serverSession = synchronized(stateLock) {
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = null
            disconnectedReleaseJob?.cancel()
            disconnectedReleaseJob = null
            val session = activeServerSession
            activeServerSession = null
            connectedSessionId = null
            session
        }
        // 重点：先立即关闭本地音视频，再异步通知服务端幂等归还 session。
        disconnectPeerOnly()
        closeServerSessionAsync(serverSession, reason)
    }

    fun release() {
        disconnect("manager_released")
        observerJob.cancel()
        peerConnectionFactory.dispose()
        audioDeviceModule.release()
        eglBase.release()
    }

    private fun bindVideoTrack(
        generation: Long,
        peer: PeerConnection,
        track: VideoTrack,
        onVideoFrame: (Bitmap) -> Unit
    ) {
        if (connectionGeneration.get() != generation || peerConnection !== peer) {
            track.setEnabled(false)
            return
        }
        videoSink?.let { oldSink ->
            remoteVideoTrack?.removeSink(oldSink)
            oldSink.release()
        }
        val sink = GreenScreenVideoSink(onVideoFrame)
        remoteVideoTrack = track
        videoSink = sink
        track.setEnabled(true)
        track.addSink(sink)
    }

    private fun bindAudioTrack(generation: Long, peer: PeerConnection, track: AudioTrack) {
        if (connectionGeneration.get() != generation || peerConnection !== peer) {
            track.setEnabled(false)
            return
        }
        remoteAudioTrack?.setEnabled(false)
        remoteAudioTrack = track
        track.setEnabled(true)
    }

    private fun disconnectPeerOnly(expectedPeer: PeerConnection? = null) {
        if (expectedPeer != null && peerConnection !== expectedPeer) return
        videoSink?.let { oldSink ->
            remoteVideoTrack?.removeSink(oldSink)
            oldSink.release()
        }
        videoSink = null
        remoteVideoTrack?.setEnabled(false)
        remoteVideoTrack = null
        remoteAudioTrack?.setEnabled(false)
        remoteAudioTrack = null
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }

    private fun handleConnectionState(
        generation: Long,
        peer: PeerConnection,
        state: PeerConnection.PeerConnectionState,
        connectionEstablished: CompletableDeferred<Unit>,
        onConnectionLost: (String) -> Unit
    ) {
        when (state) {
            PeerConnection.PeerConnectionState.CONNECTED -> {
                synchronized(stateLock) {
                    if (connectionGeneration.get() != generation || peerConnection !== peer) return
                    connectionTimeoutJob?.cancel()
                    connectionTimeoutJob = null
                    disconnectedReleaseJob?.cancel()
                    disconnectedReleaseJob = null
                }
                if (!connectionEstablished.isCompleted) connectionEstablished.complete(Unit)
            }

            PeerConnection.PeerConnectionState.DISCONNECTED -> scheduleDisconnectedRelease(
                generation = generation,
                peer = peer,
                onConnectionLost = onConnectionLost
            )

            PeerConnection.PeerConnectionState.FAILED,
            PeerConnection.PeerConnectionState.CLOSED -> {
                terminateAttempt(
                    generation = generation,
                    peer = peer,
                    reason = "webrtc_${state.name.lowercase()}",
                    onConnectionLost = onConnectionLost
                )
            }

            else -> Unit
        }
    }

    private fun scheduleConnectionTimeout(
        generation: Long,
        peer: PeerConnection,
        connectionEstablished: CompletableDeferred<Unit>,
        onConnectionLost: (String) -> Unit
    ) {
        synchronized(stateLock) {
            if (connectionGeneration.get() != generation || peerConnection !== peer) return
            if (connectionEstablished.isCompleted) return
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = observerScope.launch {
                delay(ConnectionTimeoutMillis)
                if (!connectionEstablished.isCompleted) {
                    terminateAttempt(
                        generation = generation,
                        peer = peer,
                        reason = "webrtc_connect_timeout",
                        onConnectionLost = onConnectionLost
                    )
                }
            }
        }
    }

    private fun scheduleDisconnectedRelease(
        generation: Long,
        peer: PeerConnection,
        onConnectionLost: (String) -> Unit
    ) {
        synchronized(stateLock) {
            if (connectionGeneration.get() != generation || peerConnection !== peer) return
            disconnectedReleaseJob?.cancel()
            // 重点：短暂断网保留 4 秒恢复窗口，持续 disconnected 才真正关闭服务端会话。
            disconnectedReleaseJob = observerScope.launch {
                delay(DisconnectedRecoveryMillis)
                terminateAttempt(
                    generation = generation,
                    peer = peer,
                    reason = "webrtc_disconnected_timeout",
                    onConnectionLost = onConnectionLost
                )
            }
        }
    }

    private fun terminateAttempt(
        generation: Long,
        peer: PeerConnection,
        reason: String,
        onConnectionLost: (String) -> Unit
    ) {
        val termination = invalidateAttempt(generation, peer)
        if (!termination.valid) return
        disconnectPeerOnly(peer)
        closeServerSessionAsync(termination.session, reason)
        if (termination.wasConnected) onConnectionLost(reason)
    }

    private fun invalidateAttempt(generation: Long, peer: PeerConnection): TerminationResult =
        synchronized(stateLock) {
            if (connectionGeneration.get() != generation || peerConnection !== peer) {
                return@synchronized TerminationResult(valid = false)
            }
            connectionGeneration.incrementAndGet()
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = null
            disconnectedReleaseJob?.cancel()
            disconnectedReleaseJob = null
            val result = TerminationResult(
                valid = true,
                session = activeServerSession,
                wasConnected = connectedSessionId != null
            )
            activeServerSession = null
            connectedSessionId = null
            result
        }

    private fun closeServerSessionAsync(session: ServerSession?, reason: String) {
        if (session == null) return
        cleanupScope.launch {
            runCatching {
                repository.closeSession(
                    baseUrl = session.baseUrl,
                    sessionId = session.id,
                    reason = reason
                )
            }
        }
    }

    private fun ensureConnectionCurrent(generation: Long, peer: PeerConnection) {
        if (connectionGeneration.get() != generation || peerConnection !== peer) {
            throw CancellationException("数字人连接已取消")
        }
    }

    private fun String.hasIceCandidate(): Boolean =
        lineSequence().any { it.trimStart().startsWith("a=candidate:") }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection {
        val config = PeerConnection.RTCConfiguration(emptyList())
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        return peerConnectionFactory.createPeerConnection(config, observer)
            ?: error("数字人 WebRTC PeerConnection 创建失败")
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        ensureWebRtcInitialized(appContext)
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        return PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    private fun createAudioDeviceModule(): AudioDeviceModule {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        return JavaAudioDeviceModule.builder(appContext)
            // 重点：数字人播报要走媒体音量通道，手机实体音量键才能像视频/音乐一样调节。
            .setAudioAttributes(audioAttributes)
            .setUseLowLatency(true)
            .setEnableVolumeLogger(false)
            .createAudioDeviceModule()
    }

    private fun prepareMediaAudioOutput() {
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private class DigitalHumanPeerObserver(
        private val onIceComplete: () -> Unit,
        private val onConnectionStateChange: (PeerConnection.PeerConnectionState) -> Unit,
        private val onTrack: (MediaStreamTrack?) -> Unit
    ) : PeerConnection.Observer {
        override fun onSignalingChange(newState: PeerConnection.SignalingState) = Unit
        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) = Unit
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
            onConnectionStateChange(newState)
        }
        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
            if (newState == PeerConnection.IceGatheringState.COMPLETE) onIceComplete()
        }
        override fun onIceCandidate(candidate: IceCandidate) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
        override fun onAddStream(stream: MediaStream) = Unit
        override fun onRemoveStream(stream: MediaStream) = Unit
        override fun onDataChannel(dataChannel: DataChannel) = Unit
        override fun onRenegotiationNeeded() = Unit
        override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
            onTrack(receiver.track())
        }
        override fun onTrack(transceiver: RtpTransceiver) {
            onTrack(transceiver.receiver.track())
        }
    }

    private class GreenScreenVideoSink(
        private val onFrameReady: (Bitmap) -> Unit
    ) : VideoSink {
        private val executor = Executors.newSingleThreadExecutor()
        private val busy = AtomicBoolean(false)
        @Volatile private var released = false

        override fun onFrame(frame: VideoFrame) {
            if (released || !busy.compareAndSet(false, true)) return
            frame.retain()
            try {
                executor.execute {
                    try {
                        GreenScreenKeyer.keyVideoFrame(frame)?.let(onFrameReady)
                    } finally {
                        frame.release()
                        busy.set(false)
                    }
                }
            } catch (_: RejectedExecutionException) {
                frame.release()
                busy.set(false)
            }
        }

        fun release() {
            released = true
            // 让已接收的最后一帧正常释放，禁止后续帧再进入执行器。
            executor.shutdown()
        }
    }

    private suspend fun PeerConnection.createOfferSuspend(): SessionDescription =
        suspendCancellableCoroutine { continuation ->
            createOffer(
                object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) {
                        continuation.resume(description)
                    }

                    override fun onSetSuccess() = Unit
                    override fun onCreateFailure(error: String) {
                        continuation.resumeWithException(IllegalStateException(error))
                    }

                    override fun onSetFailure(error: String) = Unit
                },
                MediaConstraints()
            )
        }

    private suspend fun PeerConnection.setLocalDescriptionSuspend(description: SessionDescription) =
        setDescriptionSuspend(description, local = true)

    private suspend fun PeerConnection.setRemoteDescriptionSuspend(description: SessionDescription) =
        setDescriptionSuspend(description, local = false)

    private suspend fun PeerConnection.setDescriptionSuspend(description: SessionDescription, local: Boolean) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val observer = object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) = Unit
                    override fun onSetSuccess() {
                        continuation.resume(Unit)
                    }

                    override fun onCreateFailure(error: String) = Unit
                    override fun onSetFailure(error: String) {
                        continuation.resumeWithException(IllegalStateException(error))
                    }
                }
                if (local) {
                    setLocalDescription(observer, description)
                } else {
                    setRemoteDescription(observer, description)
                }
            }
        }
    }

    companion object {
        private const val IceGatheringTimeoutMillis = 10_000L
        private const val ConnectionTimeoutMillis = 12_000L
        private const val DisconnectedRecoveryMillis = 4_000L
        private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        @Volatile private var initialized = false

        private fun ensureWebRtcInitialized(context: Context) {
            if (initialized) return
            synchronized(this) {
                if (initialized) return
                val options = PeerConnectionFactory.InitializationOptions.builder(context)
                    .createInitializationOptions()
                PeerConnectionFactory.initialize(options)
                initialized = true
            }
        }
    }

    private data class ServerSession(val id: Long, val baseUrl: String)

    private data class TerminationResult(
        val valid: Boolean,
        val session: ServerSession? = null,
        val wasConnected: Boolean = false
    )
}
