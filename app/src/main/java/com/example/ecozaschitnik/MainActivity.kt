package com.example.ecozaschitnik

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.ecozaschitnik.ui.EcoUi
import com.example.ecozaschitnik.ui.main.MainViewModel
import com.example.ecozaschitnik.ui.main.ReportStatus
import com.example.ecozaschitnik.ui.main.reportStatus
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var recentContainer: LinearLayout
    private lateinit var tvRecentEmpty: TextView
    private lateinit var tvEcoTip: TextView
    private lateinit var tipDots: LinearLayout

    private lateinit var tvStatTotal: TextView
    private lateinit var tvStatRecent: TextView
    private lateinit var tvStatPhoto: TextView

    private val tips: Array<String> by lazy { resources.getStringArray(R.array.eco_tips) }
    private var tipIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EcoUi.enableEdgeToEdge(this)
        setContentView(R.layout.activity_main)
        EcoUi.applySystemBarStyle(this)
        EcoUi.applySystemBarInsets(findViewById(R.id.scrollMain))

        bindGridCards()
        bindStatBlocks()
        setupTipCarousel()

        recentContainer = findViewById(R.id.recentReportsContainer)
        tvRecentEmpty = findViewById(R.id.tvRecentEmpty)

        findViewById<View>(R.id.btnCreateReport).setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }
        findViewById<View>(R.id.cardMap).setOnClickListener { openMap() }
        findViewById<View>(R.id.cardGeo).setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }
        findViewById<View>(R.id.cardPhoto).setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
        findViewById<View>(R.id.cardSite).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ecozaschitnik.web.app")))
        }
        findViewById<View>(R.id.tvSeeAll).setOnClickListener { openMap() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bindStats(state.isLoading, state.stats)
                    bindRecent(state.recentReports)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDashboard()
    }

    private fun bindGridCards() {
        bindGridInclude(
            R.id.gridMap,
            R.drawable.ic_map,
            R.string.card_map_title,
            R.string.card_map_desc,
        )
        bindGridInclude(
            R.id.gridGeo,
            R.drawable.ic_pin,
            R.string.card_geo_title,
            R.string.card_geo_desc,
        )
        bindGridInclude(
            R.id.gridPhoto,
            R.drawable.ic_camera,
            R.string.card_photo_title,
            R.string.card_photo_desc,
        )
        bindGridInclude(
            R.id.gridSite,
            R.drawable.ic_globe,
            R.string.card_site_title,
            R.string.card_site_desc,
        )
    }

    private fun bindGridInclude(includeId: Int, iconRes: Int, titleRes: Int, descRes: Int) {
        val root = findViewById<View>(includeId)
        root.findViewById<ImageView>(R.id.ivGridIcon).setImageResource(iconRes)
        root.findViewById<TextView>(R.id.tvGridTitle).setText(titleRes)
        root.findViewById<TextView>(R.id.tvGridDesc).setText(descRes)
    }

    private fun bindStatBlocks() {
        setupStat(R.id.statTotal, R.drawable.ic_map, R.string.main_stat_total) { tvStatTotal = it }
        setupStat(R.id.statRecent, R.drawable.ic_pin, R.string.main_stat_recent) { tvStatRecent = it }
        setupStat(R.id.statPhoto, R.drawable.ic_camera, R.string.main_stat_photo) { tvStatPhoto = it }
    }

    private fun setupStat(includeId: Int, iconRes: Int, labelRes: Int, onValue: (TextView) -> Unit) {
        val root = findViewById<View>(includeId)
        root.findViewById<ImageView>(R.id.ivStatIcon).setImageResource(iconRes)
        root.findViewById<TextView>(R.id.tvStatLabel).setText(labelRes)
        onValue(root.findViewById(R.id.tvStatValue))
    }

    private fun setupTipCarousel() {
        tvEcoTip = findViewById(R.id.tvEcoTip)
        tipDots = findViewById(R.id.tipDots)
        tipIndex = (tips.indices).random()
        showTip(tipIndex)
        findViewById<View>(R.id.tvEcoTip).setOnClickListener {
            tipIndex = (tipIndex + 1) % tips.size
            showTip(tipIndex)
        }
    }

    private fun showTip(index: Int) {
        tvEcoTip.text = tips[index]
        tipDots.removeAllViews()
        tips.indices.forEach { i ->
            val dot = View(this).apply {
                val size = if (i == index) 8.dp else 6.dp
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = if (i == 0) 0 else 6.dp
                }
                setBackgroundResource(
                    if (i == index) R.drawable.bg_tip_dot_active else R.drawable.bg_tip_dot_inactive,
                )
            }
            tipDots.addView(dot)
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun bindStats(loading: Boolean, stats: com.example.ecozaschitnik.ui.main.MainStats) {
        if (loading) {
            val p = getString(R.string.stat_loading)
            tvStatTotal.text = p
            tvStatRecent.text = p
            tvStatPhoto.text = p
            return
        }
        tvStatTotal.text = stats.total.toString()
        tvStatRecent.text = stats.recent30Days.toString()
        tvStatPhoto.text = stats.withPhoto.toString()
    }

    private fun bindRecent(reports: List<DumpPoint>) {
        recentContainer.removeAllViews()
        if (reports.isEmpty()) {
            tvRecentEmpty.visibility = View.VISIBLE
            return
        }
        tvRecentEmpty.visibility = View.GONE

        reports.forEachIndexed { index, dump ->
            if (index > 0) {
                recentContainer.addView(
                    View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1,
                        )
                        setBackgroundColor(getColor(R.color.dashboard_stroke))
                    },
                )
            }

            val item = layoutInflater.inflate(R.layout.item_main_recent_rich, recentContainer, false)
            item.findViewById<TextView>(R.id.tvRecentTitle).text = dump.title
            item.findViewById<TextView>(R.id.tvRecentMeta).text =
                dump.description.ifBlank { getString(R.string.card_map_desc) }
            item.findViewById<TextView>(R.id.tvRecentTime).text = formatTimeAgo(dump.timestamp)

            val badge = item.findViewById<TextView>(R.id.tvRecentBadge)
            when (dump.reportStatus()) {
                ReportStatus.OLD -> {
                    badge.setText(R.string.badge_old)
                    badge.setBackgroundResource(R.drawable.bg_badge_old)
                    badge.setTextColor(getColor(R.color.badge_old_text))
                }
                ReportStatus.RECENT -> {
                    badge.setText(R.string.badge_recent)
                    badge.setBackgroundResource(R.drawable.bg_badge_recent)
                    badge.setTextColor(getColor(R.color.badge_recent_text))
                }
                ReportStatus.NEW -> {
                    badge.setText(R.string.badge_new)
                    badge.setBackgroundResource(R.drawable.bg_badge_new)
                    badge.setTextColor(getColor(R.color.badge_new_text))
                }
            }

            val thumb = item.findViewById<ImageView>(R.id.ivRecentThumb)
            val url = dump.photoUrl
            if (!url.isNullOrBlank()) {
                thumb.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.bg_thumb_placeholder)
                    error(R.drawable.bg_thumb_placeholder)
                }
            } else {
                thumb.setImageResource(R.drawable.ic_empty_map)
                thumb.setColorFilter(getColor(R.color.app_green))
            }

            item.setOnClickListener { openMap(dump) }
            recentContainer.addView(item)
        }
    }

    private fun formatTimeAgo(timestamp: Long?): String {
        if (timestamp == null) return ""
        val diff = System.currentTimeMillis() - timestamp
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minutes < 60 -> getString(R.string.time_ago_minutes, max(1, minutes))
            hours < 24 -> getString(R.string.time_ago_hours, hours)
            days < 30 -> getString(R.string.time_ago_days, days)
            else -> getString(R.string.time_ago_months, days / 30)
        }
    }

    private fun openMap(focus: DumpPoint? = null) {
        val intent = Intent(this, MapActivity::class.java)
        focus?.let { intent.putExtra(MapActivity.EXTRA_FOCUS_REPORT, it) }
        startActivity(intent)
    }
}
