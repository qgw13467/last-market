package com.jphr.lastmarket.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.jphr.lastmarket.R
import com.jphr.lastmarket.activity.MainActivity
import com.jphr.lastmarket.adapter.MultiImageAdapter
import com.jphr.lastmarket.databinding.FragmentCreateProductBinding
import com.jphr.lastmarket.dto.CategoryDTO
import com.jphr.lastmarket.dto.LifeStyleDTO
import com.jphr.lastmarket.dto.ProductRegisterDTO
import com.jphr.lastmarket.service.ProductService
import com.jphr.lastmarket.service.UserInfoService
import com.jphr.lastmarket.util.RetrofitCallback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.time.LocalDateTime
import java.util.*


// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CreateProductFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
private const val TAG = "CreateProductFragment"

class CreateProductFragment : Fragment() {
    private var isEdit: String? = null
    private var productId: Long = 0

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf<String>(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )


    lateinit var binding: FragmentCreateProductBinding
    private lateinit var mainActivity: MainActivity
    var categoryList = mutableListOf<String>()
    var lifeStyleList = mutableListOf<String>()
    var imageUriList = mutableListOf<Uri>()
    var imageMultipartList = mutableListOf<MultipartBody.Part>()
    private lateinit var callback: OnBackPressedCallback

    var year = 0
    var month = 0
    var day = 0
    var hour = 0
    var min = 0
    var sec = 0


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mainActivity.changeFragment(1)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isEdit = it.getString("is_edit")
            productId = it.getLong("productId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCreateProductBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView: $isEdit")
        verifyStoragePermissions(requireActivity())     //권한

        UserInfoService().getCategory(CategoryCallback())
        UserInfoService().getLifeStyle(LifeStyleCallback())
        binding.radioGroup.check(R.id.live_false)
        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.live_true -> {
                    binding.startPriceLinear.visibility = View.VISIBLE
                    binding.liveStartTimeLinear.visibility = View.VISIBLE
                }
                R.id.live_false -> {
                    binding.startPriceLinear.visibility = View.GONE
                    binding.liveStartTimeLinear.visibility = View.GONE
                }
            }

        }

        var adapter: MultiImageAdapter
        val launcher: ActivityResultLauncher<Intent> = registerForActivityResult(
            StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data == null) {
                    Toast.makeText(requireContext(), "이미지를 선택하지 않았습니다.", Toast.LENGTH_LONG).show()
                } else {
                    if (data.clipData == null) {//하나만 선택
                        var imageUri = data.data
                        if (imageUri != null) {
                            val file = File(absolutelyPath(imageUri, requireContext()))  //절대경로로 바꿈
                            val requestFile =
                                RequestBody.create("image/*".toMediaTypeOrNull(), file)
                            val body =
                                MultipartBody.Part.createFormData("imgs", file.name, requestFile)

                            imageMultipartList.add(body)
                            imageUriList.add(imageUri)


                            adapter = MultiImageAdapter(mainActivity)
                            Log.d(TAG, "onCreateView: ${imageUriList.size}")
                            adapter.list = imageUriList
                            binding.recyclerview.adapter = adapter
                            var linearLayoutManager = LinearLayoutManager(context)
                            linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
                            binding.recyclerview.setLayoutManager(linearLayoutManager)

                        }
                    } else {
                        var clipData = data.clipData
                        if (clipData?.itemCount!! > 10) {
                            Toast.makeText(
                                requireContext(),
                                "사진은 10장까지 선택 가능합니다.",
                                Toast.LENGTH_LONG
                            ).show();
                        } else {

                            for (i in 0 until clipData.itemCount) {
                                val imageUri = clipData.getItemAt(i).uri // 선택한 이미지들의 uri를 가져온다.
                                val file =
                                    File(absolutelyPath(imageUri, requireContext()))  //절대경로로 바꿈
                                val requestFile =
                                    RequestBody.create("image/*".toMediaTypeOrNull(), file)
                                val body = MultipartBody.Part.createFormData(
                                    "imgs",
                                    file.name,
                                    requestFile
                                )

                                try {
                                    imageUriList.add(imageUri) //uri를 list에 담는다.
                                    imageMultipartList.add(body)        //멀티파트 리스트에 담는다.
                                } catch (e: Exception) {
                                    Log.e(TAG, "File select error", e)
                                }
                            }
                            Log.d(TAG, "onCreateView: ${imageUriList}")

                            adapter = MultiImageAdapter(mainActivity)
                            adapter.list = imageUriList
                            binding.recyclerview.adapter = adapter
                            var linearLayoutManager = LinearLayoutManager(context)
                            linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
                            binding.recyclerview.setLayoutManager(linearLayoutManager)
                        }
                    }
                }
            }
        }



        binding.imageUpload.setOnClickListener {


            var intent = Intent(Intent.ACTION_PICK)
            intent.type = MediaStore.Images.Media.CONTENT_TYPE
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            launcher.launch(intent)

        }


        binding.datePicker.setOnClickListener {

            val builder = MaterialDatePicker.Builder.datePicker() //datePicker를 만들어줍니다.
                .setTitleText("라이브 날짜 선택") //DatePicker창에 타이틀을 정해줍니다.
                .setInputMode(MaterialDatePicker.INPUT_MODE_TEXT) //첫번째 목표 이미지처럼 캘린더를 숨기고싶을때 활성화 하시면됩니다.
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())//기본 선택을정하는곳이고 오늘로 설정했습니다.

            val datePicker = builder.build()

            datePicker.addOnPositiveButtonClickListener {
                val calendar = Calendar.getInstance()
                //선택한 날짜를 Date format으로 가져오기
                calendar.time = Date(it)
                //선택한 날짜를 밀리세컨드로 값으로 가져오기
                val calendarMilli = calendar.timeInMillis
                //버튼text를에 선택한 날짜로바꿔주기
                binding.datePicker.text =
                    "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}/${
                        calendar.get(Calendar.YEAR)
                    }"

                year = calendar.get(Calendar.YEAR)
                month = calendar.get(Calendar.MONTH) + 1
                day = calendar.get(Calendar.DAY_OF_MONTH)

            }
            datePicker.show(
                mainActivity.supportFragmentManager,
                datePicker.toString()
            ) //datePicker를 보여주기

        }
        binding.timePicker.setOnClickListener {
            val picker =
                MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(12)
                    .setMinute(10)
                    .setTitleText("Select Appointment time")
                    .build()

            picker.addOnPositiveButtonClickListener {
                binding.timePicker.text =
                    picker.hour.toString() + " 시" + picker.minute.toString() + " 분"
                hour = picker.hour
                min = picker.minute
            }
            picker.show(mainActivity.supportFragmentManager, picker.toString())

            //TODO :이전 시간 선택했을 때 예외처리
        }

        binding.save.setOnClickListener {
            var prefs =
                requireActivity().getSharedPreferences("user_info", AppCompatActivity.MODE_PRIVATE)
            var token = prefs.getString("token", "")!!
            if (isEdit == "true") {     //ISEDIT가 TRUE일ㄸ ㅐ
                Log.d(TAG, "onCreateView: idedit is true")
                if (binding.radioGroup.checkedRadioButtonId == R.id.live_true) {
                    //라이브 할 때
                    var category = binding.categoryField.editText?.text.toString()
                    var content = binding.content.text.toString()
                    var instantPrice = binding.instantPrice.text.toString().toLong()
                    var lifeStyle = binding.lifestyleField.editText?.text.toString()
                    var liveTime = LocalDateTime.of(year, month, day, hour, min, sec).toString()
                    var startingPrice = binding.startPrice.text.toString().toLong()
                    var title = binding.title.text.toString()

                    try {
                        var product = ProductRegisterDTO(
                            category,
                            content,
                            instantPrice,
                            lifeStyle,
                            liveTime,
                            startingPrice,
                            title
                        )
                        var mapper: ObjectMapper = ObjectMapper()
                        var jsonString = mapper.writeValueAsString(product)
                        var jsonBody =
                            RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
                        Log.d(TAG, "onCreateView: $product")
                        ProductService().editProudct(token, productId,jsonBody, imageMultipartList)
                        mainActivity.changeFragment(1)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "입력하지 않은 항목이 있습니다", Toast.LENGTH_LONG)
                            .show()
                    }
                } else {//라이브 안할 때
                    var category = binding.categoryField.editText?.text.toString()
                    var content = binding.content.text.toString()
                    var instantPrice = binding.instantPrice.text.toString().toLong()
                    var lifeStyle = binding.lifestyleField.editText?.text.toString()
                    var title = binding.title.text.toString()


                    try {
                        var product = ProductRegisterDTO(
                            category,
                            content,
                            instantPrice,
                            lifeStyle,
                            null,
                            null,
                            title
                        )
                        var mapper: ObjectMapper = ObjectMapper()
                        var jsonString = mapper.writeValueAsString(product)
                        var jsonBody =
                            RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
                        Log.d(TAG, "onCreateView: $product")
                        ProductService().editProudct(token, productId,jsonBody, imageMultipartList)
                        mainActivity.changeFragment(1)
                    } catch (e: Exception) {
                        Log.d(TAG, "onCreateView: $e")
                        Toast.makeText(
                            requireContext(),
                            "입력하지 않은 항목이 있습니다 라이브 안할 때",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }


                }
            } else {    //ISEDIT가 FALSE 일때
                Log.d(TAG, "onCreateView: idedit is false")

                Log.d(TAG, "token logd : $token")
                Log.d(TAG, "onCreateView: ${binding.radioGroup.checkedRadioButtonId.toString()}")
                if (binding.radioGroup.checkedRadioButtonId == R.id.live_true) {
                    //라이브 할 때
                    var category = binding.categoryField.editText?.text.toString()
                    var content = binding.content.text.toString()
                    var instantPrice = binding.instantPrice.text.toString().toLong()
                    var lifeStyle = binding.lifestyleField.editText?.text.toString()
                    var liveTime = LocalDateTime.of(year, month, day, hour, min, sec).toString()
                    var startingPrice = binding.startPrice.text.toString().toLong()
                    var title = binding.title.text.toString()

                    try {
                        var product = ProductRegisterDTO(
                            category,
                            content,
                            instantPrice,
                            lifeStyle,
                            liveTime,
                            startingPrice,
                            title
                        )
                        var mapper: ObjectMapper = ObjectMapper()
                        var jsonString = mapper.writeValueAsString(product)
                        var jsonBody =
                            RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
                        Log.d(TAG, "onCreateView: $product")

                        ProductService().insertProduct(token, jsonBody, imageMultipartList)
                        mainActivity.changeFragment(1)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "입력하지 않은 항목이 있습니다", Toast.LENGTH_LONG)
                            .show()
                    }

                } else {//라이브 안할 때
                    var category = binding.categoryField.editText?.text.toString()
                    var content = binding.content.text.toString()
                    var instantPrice = binding.instantPrice.text.toString().toLong()
                    var lifeStyle = binding.lifestyleField.editText?.text.toString()
                    var title = binding.title.text.toString()


                    try {
                        var product = ProductRegisterDTO(
                            category,
                            content,
                            instantPrice,
                            lifeStyle,
                            null,
                            null,
                            title
                        )
                        var mapper: ObjectMapper = ObjectMapper()
                        var jsonString = mapper.writeValueAsString(product)
                        var jsonBody =
                            RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
                        Log.d(TAG, "onCreateView: $product")
                        ProductService().insertProduct(token, jsonBody, imageMultipartList)
                        mainActivity.changeFragment(1)
                    } catch (e: Exception) {
                        Log.d(TAG, "onCreateView: $e")
                        Toast.makeText(
                            requireContext(),
                            "입력하지 않은 항목이 있습니다 라이브 안할 때",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }


                }
//                var product=ProductRegisterDTO(category,content, instantPrice,lifeStyle, liveTime ,startingPrice,title)
//                ProductService().insertProduct(product,imageMultipartList)
//                mainActivity.changeFragment(1)

            }
        }



        return binding.root
    }
    override fun onDetach() {
        super.onDetach()
        callback.remove()
    }

    // 절대경로 변환
    fun absolutelyPath(path: Uri?, context: Context): String {
        var proj: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
        var c: Cursor? = context.contentResolver.query(path!!, proj, null, null, null)
        var index = c?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        c?.moveToFirst()

        var result = c?.getString(index!!)

        return result!!
    }

    inner class LifeStyleCallback : RetrofitCallback<LifeStyleDTO> {
        override fun onSuccess(
            code: Int,
            responseData: LifeStyleDTO,
            issearch: Boolean,
            word: String?,
            category: String?
        ) {
            if (responseData != null) {
                lifeStyleList = responseData.lifestyles
                (binding.lifestyleField.editText as? MaterialAutoCompleteTextView)?.setSimpleItems(
                    lifeStyleList.toTypedArray()
                )
                binding.lifestyle.setText(lifeStyleList[0], false)
            }
        }

        override fun onError(t: Throwable) {
            Log.d(TAG, t.message ?: "물품 정보 받아오는 중 통신오류")
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onResponse: Error Code $code")
        }

    }

    inner class CategoryCallback : RetrofitCallback<CategoryDTO> {
        override fun onSuccess(
            code: Int,
            responseData: CategoryDTO,
            issearch: Boolean,
            word: String?,
            category: String?
        ) {
            if (responseData != null) {
                categoryList = responseData.categories
                (binding.categoryField.editText as? MaterialAutoCompleteTextView)?.setSimpleItems(
                    categoryList.toTypedArray()
                )
                binding.category.setText(categoryList[0], false)

            }
        }

        override fun onError(t: Throwable) {
            Log.d(TAG, t.message ?: "물품 정보 받아오는 중 통신오류")
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onResponse: Error Code $code")
        }

    }


    fun verifyStoragePermissions(activity: Activity?) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }


}