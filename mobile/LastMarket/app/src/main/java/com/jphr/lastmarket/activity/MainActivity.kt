package com.jphr.lastmarket.activity

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.jphr.lastmarket.R
import com.jphr.lastmarket.dto.ProductDTO
import com.jphr.lastmarket.fragment.ChatFragment
import com.jphr.lastmarket.fragment.MainFragment
import com.jphr.lastmarket.fragment.MypageFragment
import com.jphr.lastmarket.fragment.SearchFragment
import com.jphr.lastmarket.service.ProductService
import com.jphr.lastmarket.service.UserInfoService
import com.jphr.lastmarket.util.RetrofitCallback


private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {
    lateinit  var drawerLayout:DrawerLayout
    lateinit var searchBar: SearchBar
    lateinit var searchView:SearchView
    var categoryList = MutableLiveData<MutableList<String>>()

    var SearchFragment=SearchFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawerLayout=findViewById<DrawerLayout>(R.id.drawer_layout)
        var navigationView=findViewById<NavigationView>(R.id.navigation_view)
        var toolbar=findViewById<MaterialToolbar>(R.id.topAppBar)
        var menu=navigationView.menu

        searchBar =findViewById(R.id.search_bar)
        searchView=findViewById<SearchView>(R.id.search_view)
        val transaction = supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, MainFragment())
        transaction.commit()

        searchBar.visibility=View.GONE


        categoryList = UserInfoService().getCategory()
        categoryList.observe(this, Observer{
            var i=0
            var groupId=1
            for (i in 0 until it.size) {
                menu.add(groupId,i,i,it[i])
                val drawable = resources.getDrawable(
                    R.drawable.circle
                )
                menu.getItem(i).icon=drawable
                if(i%3==0) groupId++
            }
        })

//        searchResult.observe(this,Observer{
//            Log.d(TAG, "observe에서 searchResult: $it")
//            var bundle= bundleOf()
//            bundle.putSerializable("products",it)
//            changeFragment(4,bundle)
//        })

        setSupportActionBar(toolbar)
        toolbar.setOnClickListener {
            changeFragment(1)
        }



        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }
        toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                android.R.id.home->{//메뉴버튼을 눌렀을 때
                    drawerLayout.openDrawer(GravityCompat.START);
                    true
                }
                R.id.search->{//검색버튼
                    if(searchBar.isVisible){
                        searchBar.visibility=View.GONE
                    }else  searchBar.visibility= View.VISIBLE
                    true
                }
                R.id.chat->{//채팅버튼
                    changeFragment(3)
                    true
                }
                R.id.mypage->{//마이페이지
                    changeFragment(2)
                    true
                }
                else ->false

            }
        }
        navigationView.setNavigationItemSelectedListener {menuItem->
            when(menuItem.itemId){
                android.R.id.home->{//메뉴버튼을 눌렀을 때
                    menuItem.isChecked = true
                    drawerLayout.close()
                    true
                }
                R.id.search->{//검색버튼
                    menuItem.isChecked = true
                    drawerLayout.close()
                    true
                }
                R.id.chat->{//채팅버튼
                    menuItem.isChecked = true
//                    changeFragment(3)
                    drawerLayout.close()
                    true
                }
                R.id.mypage->{//마이페이지
                    menuItem.isChecked = true
//                    changeFragment(2)
                    drawerLayout.close()
                    true
                }

                else ->{
                    menuItem.isChecked = true
                    drawerLayout.close()
                    false
                }
            }
        }


    }

    override fun onResume() {

        super.onResume()

        searchBar.visibility=View.GONE
        searchView
            .editText
            .setOnEditorActionListener { v, actionId, event ->
                searchBar.text = searchView.text
                searchView.hide()
                ProductService().getProduct(searchView.text.toString(),null,productCallback())
                searchBar.visibility=View.GONE
                Log.d(TAG, "onCreate: ---")
                false
            }
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when(item.itemId){
//            android.R.id.home->{//메뉴버튼을 눌렀을 때
//                drawerLayout.openDrawer(GravityCompat.START);
//            }
//            R.id.search->{//검색버튼
//                Log.d(TAG, "onOptionsItemSelected: ")
//
//            }
//            R.id.chat->{//채팅버튼
//
//            }
//            R.id.mypage->{//마이페이지
//
//            }
//
//        }
//       return super.onOptionsItemSelected(item)
//    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main,menu)
        return super.onCreateOptionsMenu(menu)
    }
    fun changeFragment(index :Int){
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        when(index){
            1->{
                transaction.replace(R.id.fragmentContainer, MainFragment()).commit()
            }
            2->{
                transaction.replace(R.id.fragmentContainer, MypageFragment()).commit()
            }
            3->{
                transaction.replace(R.id.fragmentContainer, ChatFragment()).commit()
            }
            4->{
                transaction.replace(R.id.fragmentContainer,SearchFragment).commit()
            }
        }
    }

    inner class productCallback: RetrofitCallback<ProductDTO> {
        override fun onSuccess(code: Int,responseData: ProductDTO) {
            if(responseData!=null) {

                Log.d(TAG, "initData: ${responseData}")

                var bundle= bundleOf()
                bundle.putSerializable("products",responseData)
                SearchFragment.arguments=bundle
                changeFragment(4)
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