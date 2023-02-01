package pe.edu.idat.appidatpermisocamara

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import pe.edu.idat.appidatpermisocamara.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding : ActivityMainBinding
    private var rutaFotoActual = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btntomarfoto.setOnClickListener(this)
        binding.btncompartir.setOnClickListener(this)
    }

    private fun permisoEscrituraAlmacenamiento(): Boolean{
        val resultado = ContextCompat.checkSelfPermission(applicationContext,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        var exito = false
        if(resultado == PackageManager.PERMISSION_GRANTED) exito = true
        return exito
    }

    private fun solicitarPermiso(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            1990
        )
    }

    override fun onClick(p0: View) {
        when (p0.id){
            R.id.btntomarfoto -> tomarFoto()
            R.id.btncompartir -> compartirFoto()
        }

    }
    private fun tomarFoto() {
        if(permisoEscrituraAlmacenamiento()){
            try {
                intencionTomarFoto()
            }catch (e: IOException){
                e.printStackTrace()
            }
        }else{
            solicitarPermiso()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == 1990){
            if(grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                intencionTomarFoto()
            }else{
                Toast.makeText(applicationContext,
                "Permiso denegado", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun obtenerContentURI(archivo: File): Uri {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                applicationContext,
                "pe.edu.idat.appidatpermisocamara.fileprovider",
                archivo
            )
        } else
             Uri.fromFile(archivo)

    }

    private fun intencionTomarFoto() {
        val tomarFotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(tomarFotoIntent.resolveActivity(this.packageManager) != null){
            val archivoFoto = crearArchivoTemporal()
            if(archivoFoto != null){
                val photoURI = obtenerContentURI(archivoFoto)
                tomarFotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                getResult.launch(tomarFotoIntent)
            }
        }
    }
    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){
            if(it.resultCode == Activity.RESULT_OK){
                mostrarFoto()
            }
        }
    private fun mostrarFoto(){
        val anchoIv: Int = binding.ivfoto.width
        val altoIv: Int = binding.ivfoto.height
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(rutaFotoActual, bmOptions)
        val anchoFoto = bmOptions.outWidth
        val altoFoto = bmOptions.outHeight
        val escalaImagen = min(anchoFoto / anchoIv, altoFoto / altoIv)
        bmOptions.inSampleSize = escalaImagen
        bmOptions.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeFile(rutaFotoActual, bmOptions)
        binding.ivfoto.setImageBitmap(bitmap)
    }


    private fun crearArchivoTemporal(): File{
        val nombreImagen = "JPEG_" +
                SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val directorioImagenes: File =
            this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val archivoTemporal: File = File.createTempFile(nombreImagen,
            ".jpg", directorioImagenes)
        rutaFotoActual = archivoTemporal.absolutePath
        return archivoTemporal
    }

    private fun compartirFoto() {
        if(rutaFotoActual != ""){
            val contentUri = obtenerContentURI(File(rutaFotoActual))
            val sentIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "image/jpeg"
            }
            val chooser : Intent = Intent.createChooser(sentIntent,
                "Compartir Imagen")
            if(sentIntent.resolveActivity(packageManager) != null){
                startActivity(chooser)
            }
        }else{
            Toast.makeText(applicationContext,
            "Debe seleccionar una imagen para compartirlo",
                Toast.LENGTH_LONG).show()
        }
    }




}