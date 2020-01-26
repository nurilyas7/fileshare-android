package com.nibmz7gmail.fileshare

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.nibmz7gmail.fileshare.server.Server
import fi.iki.elonen.NanoHTTPD
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.fileupload.UploadContext
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.util.Streams
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val webServer = Server(this)
        webServer.start()
    }

}