package com.example.thessense

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.example.thessense.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val lang = prefs.getString("lang", "el") ?: "el"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        window.navigationBarColor = ContextCompat.getColor(this, R.color.lightblue)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        val langItem = findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.action_language)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        updateHeaderLogo()

        // ThessSense title change
        setHomeTitle()
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.nav_home)
                    setHomeTitle()
                }
                R.id.nav_gallery -> {
                    supportActionBar?.title = "Ποιότητα Νερού"
                    navController.navigate(R.id.nav_gallery)
                }
                R.id.nav_slideshow -> {
                    supportActionBar?.title = "Ποιότητα Αέρα"
                    navController.navigate(R.id.nav_slideshow)
                }
                R.id.nav_water -> {
                    supportActionBar?.title = "Σύγκριση Νερού"
                    navController.navigate(R.id.nav_water)
                }
                R.id.nav_air -> {
                    supportActionBar?.title = "Σύγκριση Αέρα"
                    navController.navigate(R.id.nav_air)
                }

                else -> {
                    Toast.makeText(this, "Unknown item selected", Toast.LENGTH_SHORT).show()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setHomeTitle() {
        val titleText = "thesssense"
        val spannableTitle = SpannableString(titleText)
        spannableTitle.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.thess)), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableTitle.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.blue)), 5, titleText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        supportActionBar?.title = spannableTitle
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val lang = prefs.getString("lang", "el")
        val langItem = menu.findItem(R.id.action_language)
        if (lang == "el") {
            langItem.setIcon(R.drawable.ic_flag_greece)
        } else {
            langItem.setIcon(R.drawable.ic_flag_uk)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {  // Your menu item ID
                showAboutDialog()      // Call a function to show the popup
                true
            }
            R.id.action_language -> {
                val anchor = getToolbarMenuItemView(R.id.action_language) ?: findViewById(R.id.toolbar)
                showLanguagePopup(anchor)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
        val builder = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun showLanguagePopup(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "🇬🇷  Ελληνικά (GR)")
        popup.menu.add(0, 2, 1, "🇬🇧  English (EN)")
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> setLocale("el")
                2 -> setLocale("en")
            }
            true
        }
        // For icons to appear
        try {
            val fields = popup.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popup)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        popup.show()
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)


        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        prefs.edit().putString("lang", languageCode).apply()

        val langItem = findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.action_language)
        if (languageCode == "el") {
            langItem.setIcon(R.drawable.ic_flag_greece)
        } else {
            langItem.setIcon(R.drawable.ic_flag_uk)
        }

        recreate()
    }

    @SuppressLint("RestrictedApi")
    private fun getToolbarMenuItemView(menuItemId: Int): View? {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)
            if (child is androidx.appcompat.widget.ActionMenuView) {
                for (j in 0 until child.childCount) {
                    val item = child.getChildAt(j)
                    if (item is androidx.appcompat.view.menu.ActionMenuItemView) {
                        if (item.itemData?.itemId == menuItemId) {
                            return item
                        }
                    }
                }
            }
        }
        return null
    }

    private fun updateHeaderLogo() {
        val headerView = binding.navView.getHeaderView(0)
        val imageView = headerView.findViewById<ImageView>(R.id.uniLogo)
        val lang = getSharedPreferences("settings", MODE_PRIVATE).getString("lang", "el")
        if (lang == "el") {
            imageView.setImageResource(R.drawable.ihu)
        } else {
            imageView.setImageResource(R.drawable.ihu_en)
        }
    }


}