package com.example.reuseview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.reuseview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val movies = arrayListOf(
        "Django",
        "Departed",
        "Good Will Hunting",
        "The Lord of the Rings: Fellowship of the Ring",
        "Dance with the Wolves",
        "12 Angry Men"
    )

    private lateinit var binding: ActivityMainBinding
    private lateinit var moviesAdapter: ReuseView.Adapter<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        moviesAdapter = object : ReuseView.Adapter<MovieViewRetainer>() {
            override fun onCreateViewRetainer(parent: ViewGroup): MovieViewRetainer {
                val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
                return MovieViewRetainer(view)
            }

            override fun <VR> onBindViewRetainer(viewRetainer: VR, position: Int) {
                (viewRetainer as MovieViewRetainer)
                viewRetainer.tvTitle.text = movies[position]
            }

            override fun getItemCount(): Int {
                return movies.size
            }
        }


        binding.rvMovies.setAdapter(moviesAdapter)
    }


    class MovieViewRetainer(view: View) : ReuseView.ViewRetainer(view) {

        var tvTitle: TextView

        init {
            tvTitle = view.findViewById(R.id.tvMovieTitle)
        }

    }


}