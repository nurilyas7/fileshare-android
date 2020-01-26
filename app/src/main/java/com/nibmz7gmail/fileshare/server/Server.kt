package com.nibmz7gmail.fileshare.server

import android.content.Context
import android.net.wifi.WifiManager
import android.widget.Toast
import com.nibmz7gmail.fileshare.model.Event
import fi.iki.elonen.NanoHTTPD
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.fileupload.UploadContext
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.util.Streams
import timber.log.Timber
import java.io.InputStream
import java.net.BindException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException


class Server(private val context: Context) : NanoHTTPD(53725)  {

    companion object {
        const val START_SERVER: Int = 1
        const val STOP_SERVER: Int = 2
    }

    override fun start() {
        try {
            start(SOCKET_READ_TIMEOUT, false)
        } catch (e: BindException) {
            Timber.e("Port $listeningPort has already been taken")
        }
    }

    override fun start(timeout: Int, daemon: Boolean) {
        super.start(timeout, daemon)
        if(wasStarted()) {
            Timber.i("Listening on port $listeningPort")
            ServerLiveData.setStatus(Event.Success(START_SERVER))
            val wifiManger =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            val ipAddress: Inet4Address = intToInet4AddressHTH(wifiManger.connectionInfo.ipAddress)!!
            Toast.makeText(context, "${ipAddress.hostAddress} ${wifiManger.isWifiEnabled}", Toast.LENGTH_SHORT).show()
        }
    }

    fun intToInet4AddressHTH(hostAddress: Int): Inet4Address? {
        val addressBytes = byteArrayOf(
            (0xff and hostAddress).toByte(),
            (0xff and (hostAddress shr 8)).toByte(),
            (0xff and (hostAddress shr 16)).toByte(),
            (0xff and (hostAddress shr 24)).toByte()
        )
        return try {
            InetAddress.getByAddress(addressBytes) as Inet4Address
        } catch (e: UnknownHostException) {
            throw AssertionError()
        }
    }

    override fun serve(session: IHTTPSession): Response {

        return newFixedLengthResponse("{\"address\":${session.remoteIpAddress}}")

        val uri = session.uri.removePrefix("/").ifEmpty { "index.html" }

        if (uri == "upload") {
            val fileUpload = ServletFileUpload()
            val iter: FileItemIterator = fileUpload.getItemIterator(NanoHttpdContext(session))
            while (iter.hasNext()) {
                val item: FileItemStream = iter.next()
                val name = item.fieldName
                val inputStream: InputStream = item.openStream()

                if (item.isFormField) {
                    println("Form field $name with value ${Streams.asString(inputStream)} detected.")
                } else {
                    println("File field $name with file name ${item.name} detected.")
                    inputStream.use {input ->
                        val outputStream = context.openFileOutput(item.name, Context.MODE_PRIVATE)
                        outputStream.use {output ->
                            val buffer = ByteArray(4 * 1024) // buffer size
                            while (true) {
                                val byteCount = input.read(buffer)
                                if (byteCount < 0) break
                                output.write(buffer, 0, byteCount)
                            }
                            output.flush()
                        }
                    }
                }
            }
            return newFixedLengthResponse("success")
        }

        return try {
            val mime = when (uri.substringAfterLast(".")) {
                "ico" -> "image/x-icon"
                "html" -> "text/html"
                "js" -> "application/javascript"
                else -> "text"
            }
            newChunkedResponse(Response.Status.OK, mime, context.assets.open("$uri"))
        } catch (e: Exception) {
            val message = "Failed to load asset $uri because $e"
            newFixedLengthResponse(message)
        }
    }

    private class NanoHttpdContext(val session: IHTTPSession) : UploadContext {

        override fun getCharacterEncoding(): String {
            return "UTF-8"
        }

        override fun contentLength(): Long {
            val size: Long
            size = try {
                val cl1: String = session.headers["content-length"]!!
                cl1.toLong()
            } catch (var4: NumberFormatException) {
                -1L
            }

            return size
        }

        override fun getContentLength(): Int {
            return contentLength().toInt()
        }

        override fun getContentType(): String {
            return session.headers["content-type"] ?: ""
        }

        override fun getInputStream(): InputStream {
            return session.inputStream
        }

    }

}