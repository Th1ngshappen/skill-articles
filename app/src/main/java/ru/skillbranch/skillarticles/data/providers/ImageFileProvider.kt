package ru.skillbranch.skillarticles.data.providers

import android.net.Uri
import androidx.core.content.FileProvider

// так как кэш не содержит расширения, нам нужно как-то объяснить, что наш файл - изображение
// иначе (в уроке) тип файла был автоматически определён как application/octet-stream
// несмотря на то, что в EditImageContract мы указали тип Intent "image/jpeg"
class ImageFileProvider: FileProvider() {
    override fun getType(uri: Uri): String? {
        return "image/jpeg"
    }
}