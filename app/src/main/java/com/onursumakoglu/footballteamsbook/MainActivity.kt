package com.onursumakoglu.footballteamsbook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.onursumakoglu.footballteamsbook.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var teamList : ArrayList<TeamModel>
    private lateinit var teamAdapter : TeamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        teamList = ArrayList<TeamModel>()
        teamAdapter = TeamAdapter(teamList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = teamAdapter

        try {
            val database = this.openOrCreateDatabase("Teams", MODE_PRIVATE, null)

            val cursor = database.rawQuery("SELECT * FROM teams", null)
            val teamNameIx = cursor.getColumnIndex("teamname")
            val idIx = cursor.getColumnIndex("id")

            while(cursor.moveToNext()){
                val name = cursor.getString(teamNameIx)
                val id = cursor.getInt(idIx)
                val team = TeamModel(name, id)
                teamList.add(team)
            }

            teamAdapter.notifyDataSetChanged()

            cursor.close()

        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.main_options_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.add_team_item){
            val intent = Intent(this@MainActivity, TeamDetailsActivity::class.java)
            intent.putExtra("info", "new")
            startActivity(intent)
        }

        return super.onOptionsItemSelected(item)
    }


}