package com.zyron.filetree

import android.Manifest
import android.content.*
import android.content.pm.*
import android.net.*
import android.os.*
import android.provider.*
import android.content.*
import android.widget.*
import androidx.appcompat.app.*
import androidx.core.app.*
import androidx.core.view.*
import androidx.core.content.*
import androidx.recyclerview.widget.*
import androidx.drawerlayout.widget.*
import com.google.android.material.navigation.NavigationView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.zyron.filetree.adapter.*
import com.zyron.filetree.extensions.*
import com.zyron.filetree.R
import java.io.File

class MainActivity : AppCompatActivity(), FileTreeClickListener {

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private const val REQUEST_DIRECTORY_SELECTION = 2
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var toolbar: MaterialToolbar
    private lateinit var selectDirectory: MaterialButton
    private var selectedDirectory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name)

        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        toolbar.setTitle("FileTree")
        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        setupOnClickListeners()
        checkPermission()
    }

    fun setupOnClickListeners() {  
        val selectDirectory: MaterialButton = findViewById(R.id.select_directory)
        selectDirectory.setOnClickListener {
        selectDirectory()
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission()
            }
        } else {
            if (!Environment.isExternalStorageManager()) {
                requestAllFilesAccess()
            }
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE), REQUEST_EXTERNAL_STORAGE)
    }

    private fun requestAllFilesAccess() {
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "Storage access is required to browse files. Please grant permission.", Toast.LENGTH_SHORT).show()
                    requestStoragePermission()
                } else {
                    Toast.makeText(this, "Permission denied. Please allow storage access in App Settings.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:" + packageName)
                    startActivity(intent)
                }
            }
        }
    }

    private fun selectDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_DIRECTORY_SELECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_DIRECTORY_SELECTION -> {
                if (resultCode == RESULT_OK && data != null) {
                    val treeUri = data.data
                    val path = treeUri?.path?.replace("/tree/primary:", "/storage/emulated/0/")
                    val fileTree = FileTree(this, path ?: "")
                    initializeFileTree(fileTree)
                }
            }
        }
    }

    private fun initializeFileTree(fileTree: FileTree) {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view).apply {
        setItemViewCacheSize(100)
        }
        val fileTreeIconProvider = IntendedFileIconProvider()
        val fileTreeAdapter = FileTreeAdapter(this, fileTree, fileTreeIconProvider, this)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = fileTreeAdapter
        fileTree.loadFileTree()
        fileTree.setAdapterUpdateListener(object : FileTreeAdapterUpdateListener {
            override fun onFileTreeUpdated(startPosition: Int, itemCount: Int) {
                runOnUiThread {
                    fileTreeAdapter.updateNodes(fileTree.getNodes())
                    fileTreeAdapter.notifyItemRangeChanged(startPosition, itemCount)
                }
            }
        })
    }

    override fun onFileClick(file: File) {
        Toast.makeText(this, "File clicked: ${file.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onFolderClick(folder: File) {
        Toast.makeText(this, "Folder clicked: ${folder.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onFileLongClick(file: File): Boolean {
        Toast.makeText(this, "File long-clicked: ${file.name}", Toast.LENGTH_SHORT).show()
        return true 
    }

    override fun onFolderLongClick(folder: File): Boolean {
        Toast.makeText(this, "Folder long-clicked: ${folder.name}", Toast.LENGTH_SHORT).show()
        return true 
    }
}