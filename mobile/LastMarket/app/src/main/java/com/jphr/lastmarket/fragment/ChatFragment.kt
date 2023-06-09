package com.jphr.lastmarket.fragment

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jphr.lastmarket.R
import com.jphr.lastmarket.activity.MainActivity
import com.jphr.lastmarket.adapter.ChatAdapter
import com.jphr.lastmarket.adapter.ChatSocketAdapter
import com.jphr.lastmarket.databinding.FragmentChatBinding
import com.jphr.lastmarket.dto.ChatDTO
import com.jphr.lastmarket.dto.ChatLogListDTO
import com.jphr.lastmarket.dto.ProductDetailDTO
import com.jphr.lastmarket.dto.ReviewDTO
import com.jphr.lastmarket.service.ChatService
import com.jphr.lastmarket.service.MyPageService
import com.jphr.lastmarket.service.ProductService
import com.jphr.lastmarket.util.RecyclerViewDecoration
import com.jphr.lastmarket.util.RetrofitCallback
import com.jphr.lastmarket.viewmodel.MainViewModel
import io.reactivex.functions.Consumer
import org.json.JSONObject
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.StompMessage
import java.util.concurrent.atomic.AtomicBoolean


// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
private const val TAG = "ChatFragment"

class ModalBottomSheet(
    productId: Long,
    token: String,
    chatDTO: ChatDTO,
    userId: Long,
    detailDTO: ProductDetailDTO
) : BottomSheetDialogFragment() {
    var token = token
    var productId = productId
    private var stompClient: StompClient? = null
    var chatDTO = chatDTO
    var userId = userId
    var tradeId = ""
    lateinit var review: ReviewDTO
    private val wsServerUrl = "ws://i8d206.p.ssafy.io/api/ws/websocket"
    private var headerList = ArrayList<StompHeader>()
    var detailDTO=detailDTO
    fun initStomp() {
        val isUnexpectedClosed = AtomicBoolean(false)

        //stomp client 생성
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsServerUrl)

        stompClient!!.lifecycle().subscribe(Consumer { lifecycleEvent: LifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> Log.d(TAG, "Stomp connection opened")
                LifecycleEvent.Type.ERROR -> {
                    Log.e(TAG, "initStomp:")
                    Log.e(TAG, "Error", lifecycleEvent.exception)
                    if (lifecycleEvent.exception.message!!.contains("EOF")) {
                        isUnexpectedClosed.set(true)
                    }
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d(TAG, "Stomp connection closed")
                    if (isUnexpectedClosed.get()) {
                        /**
                         * EOF Error
                         */
                        /**
                         * EOF Error
                         */
                        initStomp()
                        isUnexpectedClosed.set(false)
                    }
                }
                else -> {
                    Log.d(TAG, "initStomp: else")
                }
            }
        })

        // add Header
        headerList!!.add(StompHeader("Authorization", token))
        stompClient!!.connect(headerList)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ):View? {
        setStyle(STYLE_NORMAL,R.style.CustomBottomSheetDialogTheme)
       return inflater.inflate(R.layout.chat_bottom_sheet, container, false)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //TODO:거래 완료를 판매자만 누를수있게 해야함
        initStomp()


        view?.findViewById<ImageView>(R.id.trade_sucess)?.setOnClickListener {

            //거래 성공
            val dto = ChatDTO(
                "FINISH_TRADE",
                chatDTO?.buyer.toString(),
                chatDTO?.seller.toString(),
                detailDTO.startingPrice.toString(),
                chatDTO?.roomKey.toString(),
                userId.toString()
            )
            val mapper = ObjectMapper()
            try {
                val jsonString = mapper.writeValueAsString(dto)
                stompClient!!.send("/send/room.${chatDTO?.roomKey}", jsonString).subscribe()
                Log.d(TAG, "onClick: send OK bottomfragment $jsonString")
            } catch (e: JsonProcessingException) {
                e.printStackTrace()
            }
            stompClient?.topic("/exchange/chat.exchange/room.${chatDTO?.roomKey}")
                ?.subscribe(Consumer { topicMessage: StompMessage ->
                    val str = topicMessage.payload
                    val jsonObject = JSONObject(str)
                    val type = jsonObject.getString("chatType")

                    Log.d(TAG, "onCreateView: aaaaaaaaaaaaaaaaaaaaaaaaaaa")
                    if (type.equals("FINISH_TRADE")) {
                        var chatDTO = ChatDTO(
                            jsonObject.getString("chatType"),
                            jsonObject.getString("buyer"),
                            jsonObject.getString("seller"),
                            jsonObject.getString("message"),
                            jsonObject.getString("roomKey"),
                            jsonObject.getString("sender")
                        )
                        if (!chatDTO.message.equals("FINISH_TRADE")) {//TRADE ID 가 MESSAGE에 담겨옴
                            //FINISH_TRADE는 본인이 보내는 MESSAGE
                            tradeId = chatDTO.message
                        }
                    }

                })



            val items = arrayOf("좋았어요", "그저그럼", "나빴어요")

            var selectedItem: String? = null
            val builder = AlertDialog.Builder(requireContext())
                .setTitle("거래는 어떤 느낌이였나요?")
                .setSingleChoiceItems(items, -1) { dialog, which ->
                    if (which == 0) {
                        review = ReviewDTO("GOOD", tradeId)
                    } else if (which == 1) {
                        review = ReviewDTO("SOSO", tradeId)
                    } else if (which == 2) {
                        review = ReviewDTO("BAD", tradeId)
                    }
                    selectedItem = items[which]
                }
                .setPositiveButton("OK") { dialog, which ->
                    Log.d(TAG, "onActivityCreated: $review")
                    MyPageService().insertReview(token, review)
                }
                .show()


        }
        view?.findViewById<ImageView>(R.id.trade_fail)?.setOnClickListener {
            //거래 파기
            dismiss()

        }

    }

    companion object {
        const val TAG = "ModalBottomSheet"

    }
}

class ChatFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentChatBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatSocketAdapter: ChatSocketAdapter
    private val wsServerUrl = "ws://i8d206.p.ssafy.io/api/ws/websocket"
    private var token = ""
    private var userId = 0L
    private var productId: Long = 0L
    var chatDTO: ChatDTO? = null
    private var stompClient: StompClient? = null
    private var headerList = ArrayList<StompHeader>()
    var isreservation: String = ""
    var modalBottomSheet: ModalBottomSheet? = null
    private val mainViewModel by activityViewModels<MainViewModel>()
    private lateinit var chatList: ChatLogListDTO
    private var detailDTO: ProductDetailDTO? = null
    private lateinit var callback: OnBackPressedCallback


    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mainActivity.changeFragment(1)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

        Log.d(TAG, "onAttach: ChatFragment")
        chatDTO = mainViewModel.getChatDTO()
        mainActivity = context as MainActivity
        //product 정보 불러와서 deal state에 따라 바텀시트의 상태 변화 시키기
        productId = chatDTO?.roomKey?.toLong()!!
        ProductService().getProductDetail(token,productId, ProductDetailCallback())

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        initStomp()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment'

        binding = FragmentChatBinding.inflate(inflater, container, false)

        mainActivity.actionButton.visibility=View.GONE

        var prefs =
            requireActivity().getSharedPreferences("user_info", AppCompatActivity.MODE_PRIVATE)

        token = prefs.getString("token", "")!!
        userId = prefs.getLong("user_id", 0)

        val fragManager: FragmentManager =
            requireActivity().supportFragmentManager //If using fragments from support v4


        binding.plus.setOnClickListener {
//            val modalBottomSheetBehavior = (modalBottomSheet.dialog as BottomSheetDialog).behavior
            Log.d(TAG, "onCreateView: ")
            modalBottomSheet = stompClient?.let { it1 ->
                detailDTO?.let { it2 ->
                    ModalBottomSheet(
                        productId,
                        token,
                        chatDTO!!,
                        userId,
                        it2
                    )
                }
            }
            modalBottomSheet?.show(fragManager, ModalBottomSheet.TAG)
        }
        Log.d(TAG, "onCreateView: USER ID $userId  SELLERID ${chatDTO?.seller}")
        if (chatDTO?.seller.equals(userId.toString())) {//내가 seller 일 때
            binding.nickname.text = chatDTO?.buyer
            binding.nickname.text="최강종현"
        } else {
            binding.nickname.text = chatDTO?.seller
            binding.nickname.text="호빵희종"

        }


        var roomId = chatDTO?.seller + "-" + chatDTO?.buyer + "-" + chatDTO?.roomKey

        Log.d(TAG, "onAttach: ${chatDTO}")
        initAdapter(userId)
        ChatService().getChatDetail(roomId, chatCallback())


        binding.recyclerview.apply {
            var linearLayoutManager = LinearLayoutManager(context)
            linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
            setLayoutManager(linearLayoutManager)
            adapter = chatSocketAdapter
            addItemDecoration(RecyclerViewDecoration(20, 20))
        }

        stompClient?.topic("/exchange/chat.exchange/room.${chatDTO?.roomKey}")
            ?.subscribe(Consumer { topicMessage: StompMessage ->
                val str = topicMessage.payload
                val jsonObject = JSONObject(str)
                val type = jsonObject.getString("chatType")

                if (type.equals("TRADE_CHAT")) {
                    var chatDTO = ChatDTO(
                        jsonObject.getString("chatType"),
                        jsonObject.getString("buyer"),
                        jsonObject.getString("seller"),
                        jsonObject.getString("message"),
                        jsonObject.getString("roomKey"),
                        jsonObject.getString("sender")
                    )


                    Log.d(TAG, "Chat receive 했을 때 chatDTO:  $chatDTO")
                    mainActivity.runOnUiThread {
                        binding.recyclerview.apply {
                            chatSocketAdapter.list.add(chatDTO)
                            var linearLayoutManager = LinearLayoutManager(context)
                            linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
                            setLayoutManager(linearLayoutManager)
                            adapter = chatSocketAdapter
                        }
                    }

                }

            })


        val dto = ChatDTO(
            "TRADE_CHAT",
            chatDTO?.buyer.toString(),
            chatDTO?.seller.toString(),
            "거래가 시작되었습니다",
            chatDTO?.roomKey.toString(),
            userId.toString()
        )
        val mapper = ObjectMapper()
        try {
            val jsonString = mapper.writeValueAsString(dto)
            stompClient!!.send("/send/room.${chatDTO?.roomKey}", jsonString).subscribe()
            Log.d(TAG, "onClick: send OKchatfragment$jsonString")
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }



        Log.d(TAG, "onCreateView: $chatDTO")





        binding.send.setOnClickListener {//클릭하면 send
            val dto = ChatDTO(
                "TRADE_CHAT",
                chatDTO?.buyer.toString(),
                chatDTO?.seller.toString(),
                binding.chatText.text.toString(),
                chatDTO?.roomKey.toString(),
                userId.toString()
            )

            val mapper = ObjectMapper()
            try {
                val jsonString = mapper.writeValueAsString(dto)
                stompClient!!.send("/send/room.${chatDTO?.roomKey}", jsonString).subscribe()
                Log.d(TAG, "onClick: send OK$jsonString")
            } catch (e: JsonProcessingException) {
                e.printStackTrace()
            }
//            chatSocketAdapter.list.add(dto)
//            chatSocketAdapter.notifyDataSetChanged()
            binding.chatText.text = null
        }

        return binding.root
    }

    fun initStomp() {
        val isUnexpectedClosed = AtomicBoolean(false)

        //stomp client 생성
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsServerUrl)

        stompClient!!.lifecycle().subscribe(Consumer { lifecycleEvent: LifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> Log.d(TAG, "Stomp connection opened")
                LifecycleEvent.Type.ERROR -> {
                    Log.e(TAG, "initStomp:")
                    Log.e(TAG, "Error", lifecycleEvent.exception)
                    if (lifecycleEvent.exception.message!!.contains("EOF")) {
                        isUnexpectedClosed.set(true)
                    }
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d(TAG, "Stomp connection closed")
                    if (isUnexpectedClosed.get()) {
                        /**
                         * EOF Error
                         */
                        /**
                         * EOF Error
                         */
                        initStomp()
                        isUnexpectedClosed.set(false)
                    }
                }
                else -> {
                    Log.d(TAG, "initStomp: else")
                }
            }
        })

        // add Header
        headerList!!.add(StompHeader("Authorization", token))
        stompClient!!.connect(headerList)
    }

    fun initAdapter(userId: Long) {
        chatAdapter = ChatAdapter(mainActivity)
        chatSocketAdapter = ChatSocketAdapter(mainActivity)
        chatSocketAdapter.myId = userId
        chatAdapter.myId = userId

    }

    override fun onDetach() {
        super.onDetach()
        callback.remove()

    }
    inner class chatCallback : RetrofitCallback<ChatLogListDTO> {
        override fun onSuccess(
            code: Int,
            responseData: ChatLogListDTO,
            issearch: Boolean,
            word: String?,
            category: String?
        ) {
            if (responseData != null) {
                //취미별
                chatList = responseData

                binding.recyclerviewOld.apply {
                    chatAdapter.list = responseData
                    var linearLayoutManager = LinearLayoutManager(context)
                    linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
                    linearLayoutManager.setStackFromEnd(true)
                    setLayoutManager(linearLayoutManager)
                    adapter = chatAdapter
                }
            } else Log.d(TAG, "onSuccess: data is null")
        }

        override fun onError(t: Throwable) {
            Log.d(TAG, t.message ?: "물품 정보 받아오는 중 통신오류")
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onResponse: Error Code $code")
        }

    }

    inner class ProductDetailCallback : RetrofitCallback<ProductDetailDTO> {

        override fun onSuccess(
            code: Int,
            responseData: ProductDetailDTO,
            option: Boolean,
            word: String?,
            category: String?
        ) {

            detailDTO = responseData
        }

        override fun onError(t: Throwable) {
            Log.d(TAG, t.message ?: "물품 정보 받아오는 중 통신오류")
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onResponse: Error Code $code")
        }


    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ChatFragment.
         */

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}