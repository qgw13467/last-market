package com.jphr.lastmarket.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.jphr.lastmarket.R
import com.jphr.lastmarket.activity.MainActivity
import com.jphr.lastmarket.adapter.ProductListAdapter
import com.jphr.lastmarket.databinding.FragmentSearchBinding
import com.jphr.lastmarket.dto.ListDTO
import com.jphr.lastmarket.dto.ProductDetailDTO
import com.jphr.lastmarket.dto.ProductX
import com.jphr.lastmarket.service.ProductService
import com.jphr.lastmarket.util.RecyclerViewDecoration
import com.jphr.lastmarket.util.RetrofitCallback
import com.jphr.lastmarket.viewmodel.MainViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
private const val TAG = "SearchFragment"
class SearchFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var productDTO: MutableList<ProductX>? = null
    private var word: String? = null
    private lateinit var binding:FragmentSearchBinding
    private lateinit var productListAdapter: ProductListAdapter
    private lateinit var mainActivity: MainActivity
    private val mainViewModel by activityViewModels<MainViewModel>()
    lateinit var cityData:String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity=context as MainActivity
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            productDTO = it.getSerializable("products") as ProductDTO?
//            word = it.getString("word")
//        }
        productDTO=mainViewModel.getProduct()
        word=mainViewModel.getWord()
        var pref=mainActivity.getSharedPreferences("user_info", AppCompatActivity.MODE_PRIVATE)
        cityData= pref?.getString("city_data","null").toString()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding=FragmentSearchBinding.inflate(inflater,container,false)

        productListAdapter= ProductListAdapter(mainActivity)
        binding.recyclerview.apply {
            productListAdapter.list=productDTO as MutableList<ProductX>
            layoutManager= GridLayoutManager(context,3)
            adapter=productListAdapter
            addItemDecoration(RecyclerViewDecoration(60,0))
        }
        binding.resultText.text="${word}에 대한 검색결과"

        val itemList=resources.getStringArray(R.array.sort)
        binding.spinner.adapter= ArrayAdapter(requireContext(), com.gun0912.tedpermission.R.layout.support_simple_spinner_dropdown_item,itemList)

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                try {
                    if (binding.spinner.getItemAtPosition(position).toString().substring(0, 3) == "최신순"){
                        ProductService().getProductWithSort(null,null,cityData,"createdDateTime,DESC","","0",ProductCallback(),true,word)
                        Log.d(TAG, "onItemSelected: 최신순")
                    }else if(binding.spinner.getItemAtPosition(position).toString() =="찜순"){
                        ProductService().getProductWithSort(null,null,cityData,"favoriteCnt,DESC","","0",ProductCallback(),true,word)
                        Log.d(TAG, "onItemSelected: 찜순")

                    }else if(binding.spinner.getItemAtPosition(position).toString().substring(0, 4) == "라이브중"){
                        ProductService().getProductWithSort(null,null,cityData,"favoriteCnt,DESC","ONBROADCAST","0",ProductCallback(),true,word)
                        Log.d(TAG, "onItemSelected: 라이브중")

                    }

                } catch(e: Exception) {
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) = Unit
        }


        productListAdapter.setItemClickListener(object : ProductListAdapter.ItemClickListener{
            override fun onClick(view: View, position: Int) {
                productListAdapter.list?.get(position)?.productId
                    ?.let {
                        ProductService().getProductDetail(it,ProductDetailCallback())
                    }

            }
        })
        return binding.root

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SearchFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SearchFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    inner class ProductDetailCallback: RetrofitCallback<ProductDetailDTO> {

        override fun onSuccess(
            code: Int,
            responseData: ProductDetailDTO,
            option: Boolean,
            word: String?,
            category: String?
        ) {

            mainViewModel.setProductDetail(responseData)
            mainActivity!!.changeFragment(7)
        }

        override fun onError(t: Throwable) {
            Log.d(TAG, t.message ?: "물품 정보 받아오는 중 통신오류")
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onResponse: Error Code $code")
        }


    }
    inner class ProductCallback: RetrofitCallback<ListDTO> {
        override fun onSuccess(code: Int, responseData: ListDTO, issearch:Boolean, word:String?, category:String?) {
            if(responseData!=null) {
                if(issearch){
                    mainViewModel.setProduct(responseData.content)
                    if (word != null) {
                        mainViewModel.setWord(word)
                        productListAdapter.list=responseData.content
                        productListAdapter.notifyDataSetChanged()

                    }
                }
                else {
                    mainViewModel.setProduct(responseData.content)
                    if (category != null) {
                        mainViewModel.setCategory(category)
                        productListAdapter.list=responseData.content
                        productListAdapter.notifyDataSetChanged()
                        Log.d(TAG, "onSuccess: 콜백!!!!!!!!!!!")


                    }
                }
            }
        }

        override fun onError(t: Throwable) {
            Log.d(TAG, t.message ?: "물품 정보 받아오는 중 통신오류")
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onResponse: Error Code $code")
        }

    }
}