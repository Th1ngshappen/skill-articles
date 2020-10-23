package ru.skillbranch.skillarticles.ui.profile

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

class EditImageContract : ActivityResultContract<Pair<Uri, Uri>, Uri?>() {

    override fun createIntent(context: Context, input: Pair<Uri, Uri>?): Intent {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(input!!.first, "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(MediaStore.EXTRA_OUTPUT, input.second)
            clipData = ClipData.newUri(context.contentResolver, "A photo", input.second)
            // а без этого разрешения внешнее приложение не сможет считать из этого uri ни одного пикселя
            // считает 0 и напишет, что не может редактировать изображения размером меньше 50x50 px
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            putExtra("return-value", true)
        }

        return Intent.createChooser(intent, "Choose application for edit avatar")

//        // scan all applications on device for resolve this intent action (action Intent.ACTION_EDIT) type "image/jpeg"
//        val resolveInfoList = context.packageManager
//            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
//            .map { info -> info.activityInfo.packageName }
//
//        // предоставляем права всем приложениям, которые способны обработать этот интент
//        resolveInfoList.forEach { resolvePackage ->
//            context.grantUriPermission(
//                resolvePackage, // for package - для кого разрешаем
//                input!!.second, // for destination uri - uri, куда будем записывать
//                Intent.FLAG_GRANT_WRITE_URI_PERMISSION // grant permission for write to destination - какое разрешение даём
//            )
//        }

    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data
        else null
    }

}