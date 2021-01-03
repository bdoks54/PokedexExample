package com.example.paging;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PageKeyedDataSource;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableField;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.paging.databinding.ActivityMainBinding;
import com.example.paging.databinding.ItemRecyclerviewBinding;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PokeAPI pokeAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        //recyclerView = findViewById(R.id.recyclerView);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        final MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter();
        binding.recyclerView.setAdapter(adapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);

        //레트로핏 초기화 과정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")  //변하지 않는 주소의 앞부분
                .addConverterFactory(GsonConverterFactory.create()) //어떤 컨버터를 사용할 지 지정 - Gson
                .build();
        pokeAPI = retrofit.create(PokeAPI.class);

        createLiveData().observe(this, results -> {
            adapter.submitList(results);
        });
    }

    private class DataSource extends PageKeyedDataSource<String, Result>{
        //params를 활용하여 loadInitial 메서드 내에서 자료를 얻은 후 자료와 키들을 callback.onResult로 전달
        @Override
        public void loadInitial(@NonNull LoadInitialParams<String> params, @NonNull LoadInitialCallback<String, Result> callback) {
            try {
                Response body = pokeAPI.listPokemons().execute().body();
                callback.onResult(body.results, body.previous, body.next);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void loadBefore(@NonNull LoadParams<String> params, @NonNull LoadCallback<String, Result> callback) {
            String queryPart = params.key.split("\\?")[1];
            String[] queries = queryPart.split("&");
            Map<String, String> map = new HashMap<>();
            for (String query : queries){
                String[] splited = query.split("=");
                map.put(splited[0], splited[1]);
            }
            try {
                Response body = pokeAPI.listPokemons(map.get("offset"), map.get("limit")).execute().body();
                callback.onResult(body.results, body.previous);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void loadAfter(@NonNull LoadParams<String> params, @NonNull LoadCallback<String, Result> callback) {
            String queryPart = params.key.split("\\?")[1];
            String[] queries = queryPart.split("&");
            Map<String, String> map = new HashMap<>();
            for (String query : queries) {
                String[] splited = query.split("=");
                map.put(splited[0], splited[1]);
            }
            try {
                Response body = pokeAPI.listPokemons(map.get("offset"), map.get("limit")).execute().body();
                callback.onResult(body.results, body.next);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private LiveData<PagedList<Result>> createLiveData(){
        PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(20) //첫번째 로딩할 사이즈, 정의하지 않으면 페이지 사이즈의 3배를 가져온다. PositionalDataSource에서는 이 값을 2배 이상으로 설정해야하며, 다른타입의 데이터소스에는 이런 제약이 없다.
                .setPageSize(20)    //페이지 사이즈
                .setPrefetchDistance(10)    //몇 개의 데이터가 남았을 때 새로운 페이지를 로딩할 지 결정한다.
                .build();

        return new LivePagedListBuilder<>(new androidx.paging.DataSource.Factory<String, Result>(){
            @Override
            public androidx.paging.DataSource<String, Result> create(){
                return new MainActivity.DataSource();
            }
        }, config).build();
    }

    private static class MainRecyclerViewAdapter extends PagedListAdapter<Result, MainRecyclerViewViewHolder>{
        protected MainRecyclerViewAdapter(){
            super(new DiffUtil.ItemCallback<Result>() {
                @Override
                public boolean areItemsTheSame(@NonNull Result oldItem, @NonNull Result newItem) {
                    return oldItem.name.equals(newItem.name);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Result oldItem, @NonNull Result newItem) {
                    return oldItem.name.equals(newItem.name) && oldItem.url.equals(newItem.url);
                }
            });
        }

        @NonNull
        @Override
        public MainRecyclerViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRecyclerviewBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                    R.layout.item_recyclerview, parent, false);
            return new MainRecyclerViewViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull MainRecyclerViewViewHolder holder, int position) {
            Result item = getItem(position);
            holder.bind(item);
        }
    }

    private static class MainRecyclerViewViewHolder extends RecyclerView.ViewHolder{
        private final ViewModel viewModel;

        //데이터 바인딩을 사용하는 뷰 홀더 생성자
        public MainRecyclerViewViewHolder(ItemRecyclerviewBinding binding){
            super(binding.getRoot());
            viewModel = new ViewModel();
            binding.setViewModel(viewModel);
        }

        //모델을 받아 뷰 모델을 채우는 bind 메서드
        public void bind(Result item){
            viewModel.name.set(item.name);
            viewModel.url.set(item.url);
        }
    }


    public static class ViewModel{

        public ObservableField<String> name = new ObservableField<>();
        public ObservableField<String> url = new ObservableField<>();
    }
}