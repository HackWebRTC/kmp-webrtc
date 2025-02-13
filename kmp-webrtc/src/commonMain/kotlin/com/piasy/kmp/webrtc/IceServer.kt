package com.piasy.kmp.webrtc

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-17.
 */
data class IceServer(
  val urls: List<String>,
  val username: String,
  val password: String,
  val tlsCertPolicy: Int,
  val hostname: String,
  val tlsAlpnProtocols: List<String>,
  val tlsEllipticCurves: List<String>
) {
  companion object {
    const val TLS_CERT_POLICY_SECURE = 1
    const val TLS_CERT_POLICY_INSECURE_NO_CHECK = 2
  }
}
