package com.example.buscamines


import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest

class Perfil : AppCompatActivity() {

    lateinit var auth: FirebaseAuth

    /*buttons*/
    lateinit var backBtn: Button
    lateinit var canviarimatgeBtn: Button

    /*text menu*/
    lateinit var miPuntuaciotxt: TextView
    lateinit var puntuacio: TextView
    lateinit var join_date: TextView

    lateinit var correo: TextView
    lateinit var nom: TextView
    lateinit var uid: String
    var user: FirebaseUser? = null;
    lateinit var imatgePerfil: ImageView
    lateinit var storageReference: StorageReference
    lateinit var folderReference: StorageReference
    private var currentPhotoPath: String = ""
    private var uidRankingPlayer:String=""
    private var uidJugador:String=""
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    lateinit var imatgeUri: Uri
    private lateinit var activityResultLauncher: ActivityResultLauncher<String>
    //It converts the bitmap into uri and upload the image
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                val imageBitmap = intent?.extras?.get("data") as Bitmap
                imatgeUri = bitmapToUri(imageBitmap)
                imatgePerfil.setImageURI(imatgeUri)
                pujarFoto(imatgeUri)
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)
        //Initialize the Firebase Storage
        storageReference = FirebaseStorage.getInstance().getReference()
        folderReference = storageReference.child("FotosPerfil")
        //Gallery
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    // La selección de la imagen fue exitosa
                    imatgeUri = uri
                    Log.d("ActivityResult", "URI de la imagen seleccionada: $imatgeUri")
                    imatgePerfil.setImageURI(imatgeUri)
                    pujarFoto(imatgeUri)


                }
            }

        canviarimatgeBtn = findViewById<Button>(R.id.canviar_imatge)
        backBtn = findViewById<Button>(R.id.back_btn)

        auth = FirebaseAuth.getInstance()
        user = auth.currentUser
        uid= user?.uid.toString()

        imatgePerfil  = findViewById(R.id.imatgePerfil)

        /*font*/
        val tf = Typeface.createFromAsset(assets, "fonts/Fredoka-Medium.ttf")

        /*text menu*/
        miPuntuaciotxt = findViewById(R.id.miPuntuaciotxt)
        puntuacio = findViewById(R.id.puntuacio)
        correo = findViewById(R.id.correo)
        nom = findViewById(R.id.nom)
        join_date = findViewById(R.id.fecha_union)

        /*text menu*/
        miPuntuaciotxt.setTypeface(tf)
        puntuacio.setTypeface(tf)
        correo.setTypeface(tf)
        nom.setTypeface(tf)
        canviarimatgeBtn.setTypeface(tf)
        backBtn.setTypeface(tf)
        join_date.setTypeface(tf)

        /*buttons*/
        backBtn.setOnClickListener {
            val intent = Intent(this, Menu::class.java)
            startActivity(intent)
            finish()
        }
        canviarimatgeBtn.setOnClickListener {
            canviaLaImatge()
        }
        //Retrieves the uid's
        var intent:Bundle? = getIntent().extras
        uidRankingPlayer = intent?.get("UID_RankingPlayer").toString()
        uidJugador=intent?.get("UID_Jugador").toString()


        if(uidJugador==uid|| uidRankingPlayer==uidJugador){
            consulta()

        }
        else{
            canviarimatgeBtn.visibility= View.GONE
            consultaRanking(uidRankingPlayer)
            backBtn.setOnClickListener {
                val intent = Intent(this, Ranking::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val REQUEST_CODE = 201
        Log.d("ActivityResult", "Request Code: $requestCode, Result Code: $resultCode")
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                imatgeUri = data?.data!!
                Log.d("ActivityResult", "URI de la imagen seleccionada: $imatgeUri")
                imatgePerfil.setImageURI(imatgeUri)
            } else {
                Log.e("ActivityResult", "Error: Result Code no es RESULT_OK")

            }
        } else {
            Log.e("ActivityResult", "Error: Request Code no es el esperado")
        }
    }


    override fun onStart() {
        usuariLogejat()
        super.onStart()
    }


    private fun usuariLogejat() {
        if (user != null) {

        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    //Fills the player information when the user comes from the menu
    private fun consulta() {
        var database: FirebaseDatabase =
            FirebaseDatabase.getInstance("https://buscamines-11db7-default-rtdb.europe-west1.firebasedatabase.app/")
        var bdreference: DatabaseReference = database.getReference("DATA BASE JUGADORS")
        bdreference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                var trobat: Boolean = false
                for (ds in snapshot.getChildren()) {

                    if (ds.child("Email").getValue().toString().equals(user?.email)) {
                        trobat = true
                        puntuacio.setText(ds.child("Puntuacio").getValue().toString())
                        correo.setText(ds.child("Email").getValue().toString())
                        nom.setText(ds.child("Nom").getValue().toString())
                        uid = (ds.child("Uid").getValue().toString())
                        join_date.setText(getString(R.string.fecha_union)+" "+ds.child("Data").getValue().toString())

                        val imageReference = folderReference.child(uid)

                        imageReference.downloadUrl.addOnSuccessListener { uri ->
                            val url = uri.toString()
                            try {
                                Picasso.get().load(url).into(imatgePerfil)
                            } catch (e: Exception) {
                                Picasso.get().load(R.drawable.profile_pic).into(imatgePerfil)
                            }
                        }


                    }

                    if (!trobat) {
                        Log.e("ERROR", "ERROR NO TROBAT MAIL")
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ERROR", "ERROR DATABASE CANCEL")
            }
        })
    }
    //When this activity comes from Ranking, this function fills the players information thanks to the UID player
    private fun consultaRanking(uid: String) {
        val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://buscamines-11db7-default-rtdb.europe-west1.firebasedatabase.app/")
        val bdreference: DatabaseReference = database.getReference("DATA BASE JUGADORS")

        bdreference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var trobat: Boolean = false
                for (ds in snapshot.children) {
                    if (ds.child("Uid").getValue(String::class.java) == uid) {
                        trobat = true
                        // Obtener la puntuación, el correo y el nombre del usuario usando el UID
                        val puntuacion = ds.child("Puntuacio").getValue(String::class.java)
                        val correoe = ds.child("Email").getValue(String::class.java)
                        val nombre = ds.child("Nom").getValue(String::class.java)

                        // Asignar la información obtenida a las vistas correspondientes
                        puntuacio.setText(puntuacion)
                        correo.setText(correoe)
                        nom.setText(nombre)
                        join_date.setText(getString(R.string.fecha_union)+" "+ds.child("Data").getValue().toString())


                        // Obtener la URL de la imagen del usuario
                        val imageReference = folderReference.child(uid)

                        imageReference.downloadUrl.addOnSuccessListener { uri ->
                            // URL de descarga obtenida con éxito
                            val url = uri.toString()
                            // Cargar la imagen en el ImageView usando Picasso u otra biblioteca
                            Picasso.get().load(url).into(imatgePerfil)
                        }.addOnFailureListener { exception ->
                            // Si ocurre un error al cargar la imagen, puedes mostrar una imagen de perfil predeterminada
                            Picasso.get().load(R.drawable.profile_pic).into(imatgePerfil)
                        }
                    }
                }

                if (!trobat) {
                    Log.e("ERROR", "Usuario no encontrado con el UID proporcionado")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ERROR", "Error al acceder a la base de datos: ${error.message}")
            }
        })
    }

    fun isPermissionsAllowed(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            false
        } else true
    }
//Asks gallery permission
    fun askForPermissions(): Boolean {
        val REQUEST_CODE = 201
        if (!isPermissionsAllowed()) {
            if
                    (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                showPermissionDeniedDialog()
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ), REQUEST_CODE
                )
            }
            return false
        }
        return true
    }
//When the permission is granted this function opens the gallery or camera
    override fun onRequestPermissionsResult(
        requestCode:
        Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions,
            grantResults
        )
        val REQUEST_CODE = 201
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.size > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    // permission is granted, you can perform your operation here
                } else {
                    // permission is denied, you can ask for permission again, if you want
                    //  askForPermissions()
                }
                return
            }
        }
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso concedido, inicia la captura de imágenes
                    startCamera()
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
                }
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Permission is denied, Please allow permissions from App Settings.")
            .setPositiveButton("App Settings",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    // send to app settings if permission is denied permanently
                    val intent = Intent()
                    intent.action =
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        getPackageName(), null
                    )
                    intent.data = uri
                    startActivity(intent)
                })
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun canviaLaImatge() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_title))
            .setMessage(getString(R.string.dialog_message))
            .setNegativeButton(getString(R.string.dialog_gallery)) { view, _ ->
                if (askForPermissions()) {
                    activityResultLauncher.launch("image/*")
                }
            }
            .setPositiveButton(getString(R.string.dialog_camera)) { _, _ ->
                val cameraPermission = Manifest.permission.CAMERA

                // Checks if the permission is granted
                if (ContextCompat.checkSelfPermission(this, cameraPermission) == PackageManager.PERMISSION_GRANTED) {
                    // Si ya tienes permisos, inicia la captura de imágenes
                    startCamera()
                } else {
                    // If not, asks for it
                    ActivityCompat.requestPermissions(this, arrayOf(cameraPermission), CAMERA_PERMISSION_REQUEST_CODE)
                }


            }
            .setCancelable(false)
            .create()
        dialog.show()
    }
//Upload the image in the Storage
    private fun pujarFoto(imatgeUri: Uri) {

        var Uids: String = uid
        //Podriem fer:
        //folderReference.child(Uids).putFile(imatgeUri)
        //Pero utilitzem el mètode recomanat a la documentació
        //      https://firebase.google.com/docs/storage/android/upload files
        // Get the data from an ImageView as bytes
        imatgePerfil.isDrawingCacheEnabled = true
        imatgePerfil.buildDrawingCache()
        val bitmap = (imatgePerfil.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        var uploadTask = folderReference.child(Uids).putBytes(data)
        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads

        }.addOnSuccessListener { taskSnapshot ->
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            // ...
        }
    }

    //It converts bitmap to uri
    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    //Start the camera activity
    private fun startCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startForResult.launch(intent)
        }
    }



}
