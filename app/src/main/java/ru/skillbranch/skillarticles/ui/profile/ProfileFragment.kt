package ru.skillbranch.skillarticles.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.base.MenuItemHolder
import ru.skillbranch.skillarticles.ui.base.ToolbarBuilder
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.ui.dialogs.AvatarActionsDialog
import ru.skillbranch.skillarticles.ui.dialogs.EditProfileDialog
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.profile.PendingAction
import ru.skillbranch.skillarticles.viewmodels.profile.ProfileState
import ru.skillbranch.skillarticles.viewmodels.profile.ProfileViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ProfileFragment() : BaseFragment<ProfileViewModel>() {

    override val viewModel: ProfileViewModel by activityViewModels()
    override val layout: Int = R.layout.fragment_profile
    override val binding: ProfileBinding by lazy { ProfileBinding() }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var permissionLauncher: ActivityResultLauncher<Array<out String>>

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var cameraLauncher: ActivityResultLauncher<Uri>

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var galleryLauncher: ActivityResultLauncher<String>

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var editPhotoLauncher: ActivityResultLauncher<Pair<Uri, Uri>>

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var settingsLauncher: ActivityResultLauncher<Intent>

    override val prepareToolbar: (ToolbarBuilder.() -> Unit)? = {
        addMenuItem(
            MenuItemHolder(
                "edit",
                R.id.action_edit,
                R.drawable.ic_baseline_edit_24
            ) {
                val action = ProfileFragmentDirections
                    .actionNavProfileToDialogEditProfile(binding.name, binding.about)
                viewModel.navigateWithAction(action)
            }
        )
        addMenuItem(
            MenuItemHolder(
                "logout",
                R.id.action_logout,
                R.drawable.ic_baseline_exit_to_app_24
            ) {
                viewModel.handleLogout()
            }
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val resultRegistry = requireActivity().activityResultRegistry

        permissionLauncher = registerForActivityResult(
            RequestMultiplePermissions(),
            resultRegistry,
            ::callbackPermissions
        )
        cameraLauncher = registerForActivityResult(TakePicture(), resultRegistry, ::callbackCamera)
        galleryLauncher = registerForActivityResult(GetContent(), resultRegistry, ::callbackGallery)
        editPhotoLauncher =
            registerForActivityResult(EditImageContract(), resultRegistry, ::callbackEditPhoto)
        settingsLauncher =
            registerForActivityResult(StartActivityForResult(), resultRegistry, ::callbackSettings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // listen for fragment result
        setFragmentResultListener(AvatarActionsDialog.AVATAR_ACTIONS_KEY) { _, bundle ->
            when (bundle[AvatarActionsDialog.SELECT_ACTION_KEY] as String) {
                AvatarActionsDialog.CAMERA_KEY -> viewModel.handleCameraAction(prepareTempUri())
                AvatarActionsDialog.GALLERY_KEY -> viewModel.handleGalleryAction()
                AvatarActionsDialog.DELETE_KEY -> viewModel.handleDeleteAction()
                AvatarActionsDialog.EDIT_KEY -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        // глайд хэширует изображение в кэше приложения
                        // получаем это изображение из кэша
                        // синхронный метод нужно вызывать на бэкграунт потоке, чтобы не блочить UI,
                        // иначе получим исключение
                        // 12: 01:37:10 можно было бы воспользоваться также glide ? лиснером,
                        // если бы мы сейчас не делали всё в контексте корутин
                        // Glide submit get it is sync call, don't call on UI thread
                        val sourceFile = Glide.with(requireActivity())
                            .asFile()
                            .load(binding.avatar)
                            .submit()
                            .get()

                        val sourceUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider", // .provider - то, что объявили в манифесте
                            sourceFile
                        )

                        withContext(Dispatchers.Main) {
                            // sourceUri - фотография, которую  должны обработать
                            // 2 параметр - адрес, куда будет сохранено обработанное изображение
                            viewModel.handleEditAction(sourceUri, prepareTempUri())
                        }
                    }
                }
            }
        }
        setFragmentResultListener(EditProfileDialog.EDIT_PROFILE_KEY) { _, bundle ->
            val name = bundle[EditProfileDialog.APPLIED_NAME] as String
            val about = bundle[EditProfileDialog.APPLIED_ABOUT] as String
            viewModel.handleEditProfile(name, about)
        }
        setHasOptionsMenu(true)
    }

    override fun setupViews() {
        iv_avatar.setOnClickListener {
            val action =
                ProfileFragmentDirections.actionNavProfileToDialogAvatarActions(binding.avatar.isNotBlank())
            viewModel.navigateWithAction(action)
        }

        viewModel.observePermissions(viewLifecycleOwner) {
            // launch callback for request permissions
            permissionLauncher.launch(it.toTypedArray())
        }

        viewModel.observeActivityResults(viewLifecycleOwner) {
            when (it) {
                is PendingAction.GalleryAction -> galleryLauncher.launch(it.payload)
                is PendingAction.SettingsAction -> settingsLauncher.launch(it.payload)
                is PendingAction.CameraAction -> cameraLauncher.launch(it.payload)
                is PendingAction.EditAction -> editPhotoLauncher.launch(it.payload)
            }
        }
    }

    private fun updateAvatar(avatarUrl: String) {
        if (avatarUrl.isBlank()) {
            Glide.with(this)
                .load(R.drawable.ic_avatar)
                .into(iv_avatar)
        } else {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar)
                .apply(RequestOptions.circleCropTransform())
                .into(iv_avatar)
        }
    }

    // 12: 01:27:45
    // так как мы работаем в контексте одного приложения, мы могли бы обойтись просто созданием одного файла,
    // но мы хотим на будущее, чтобы название всех uri были однотипными, потому что в дальнейшем,
    // если мы захотим обработать temp uri, то лучше, чтобы он был доступен именно в виде content uri
    // и мы могли бы его передать в какое-то внешнее приложение для последующей обработки
    // например, открываем камеру, получаем от неё текущий uri и сразу же пробрасываем его
    // в другое приложение, которое отредактирует эту фотографию
    // поэтому, чтобы унифицировать этот механизм, мы создаём метод prepareTempUri(), который будет возвращать
    // именно content uri, который мы будем получать из FileProvider'а
    @VisibleForTesting
    fun prepareTempUri(): Uri {
        // timestamp потому что нужна какая-то уникальная строка
        val timestamp = SimpleDateFormat("HHmmss").format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // create empty temp file with a unique name
        // createTempFile создаёт просто пустой файл с файловым дескриптором
        val tempFile = File.createTempFile(
            "JPEG_${timestamp}",
            ".jpg",
            storageDir
        )

        // must return content: uri not file: uri
        // file: uri нельзя расшарить другому приложению на android выше 7
        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider", // .provider - то, что объявили в манифесте
            tempFile
        )

        Log.d("M_ProfileFragment", "file uri: ${tempFile.toUri()} content uri: $contentUri")
        return contentUri
    }

    @VisibleForTesting
    fun removeTempUri(uri: Uri?) {
        uri ?: return
        requireContext().contentResolver.delete(uri, null, null)
    }

    private fun callbackPermissions(result: Map<String, Boolean>) {

        // shouldShowRequestPermissionRationale используется для определения,
        // можем ли мы показать пользователю запрос на разрешение
        val permissionsResult = result.mapValues { (permission, isGranted) ->
            if (isGranted) true to true
            else false to ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                permission
            )
        }
        // remove temp file by uri if permissions denied
        val isAllGranted = !permissionsResult.values.map { it.first }.contains(false)
        if (!isAllGranted) {
            val tempUri = when (val pendingAction = binding.pendingAction) {
                is PendingAction.CameraAction -> pendingAction.payload
                is PendingAction.EditAction -> pendingAction.payload.second
                else -> null
            }
            removeTempUri(tempUri)
        }

        viewModel.handlePermission(permissionsResult)
    }

    private fun callbackCamera(result: Boolean) {
        val (payload) = binding.pendingAction as PendingAction.CameraAction
        // if take photo from camera upload to server
        if (result) {
            val inputStream = requireContext().contentResolver.openInputStream(payload)
            viewModel.handleUploadPhoto(inputStream)
        } else {
            // else remove temp uri
            removeTempUri(payload)
        }
    }

    private fun callbackGallery(result: Uri?) {
        if (result != null) {
            val inputStream = requireContext().contentResolver.openInputStream(result)
            viewModel.handleUploadPhoto(inputStream)
        }
    }

    private fun callbackEditPhoto(result: Uri?) {
        if (result != null) {
            val inputStream = requireContext().contentResolver.openInputStream(result)
            viewModel.handleUploadPhoto(inputStream)
        } else {
            // else remove temp uri
            val (payload) = binding.pendingAction as PendingAction.EditAction
            removeTempUri(payload.second)
        }
    }

    private fun callbackSettings(result: ActivityResult) {
        // TODO do something после возвращения из настроек, например показать диалоговое окно с выбором экшена
    }

    inner class ProfileBinding : Binding() {

        var pendingAction: PendingAction? = null

        var avatar by RenderProp("") {
            updateAvatar(it)
        }

        var name by RenderProp("") {
            tv_name.text = it
        }
        var about by RenderProp("") {
            tv_about.text = it
        }
        var rating by RenderProp(0) {
            tv_rating.text = "Rating: $it"
        }
        var respect by RenderProp(0) {
            tv_respect.text = "Respect: $it"
        }

        override fun bind(data: IViewModelState) {
            data as ProfileState
            if (data.avatar != null) avatar = data.avatar
            if (data.name != null) name = data.name
            if (data.about != null) about = data.about
            rating = data.rating
            respect = data.respect
            pendingAction = data.pendingAction
        }

    }

}
