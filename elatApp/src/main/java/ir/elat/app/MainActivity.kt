package ir.elat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import ir.elat.app.domain.NewsArticle
import ir.elat.app.domain.Reaction
import ir.elat.app.domain.ReadingLayer
import ir.elat.app.feature.feed.FeedViewModel
import ir.elat.app.ui.*
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ElatTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var showSplash by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        delay(1300)
                        showSplash = false
                    }
                    if (showSplash) Splash() else ElatApp()
                }
            }
        }
    }
}

@Composable
private fun Splash() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF052E53), Color(0xFF0A5A93), Color(0xFFEAF8F0)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("ELAT", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Black)
                Text(".ir", color = ElatGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text("خبرگزاری اقتصادی", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("نبض اقتصاد، آینده‌ای روشن", color = Color(0xFFDFF5E7), fontSize = 18.sp)
            Spacer(Modifier.height(34.dp))
            CircularProgressIndicator(color = ElatGreen)
        }
    }
}

@Composable
private fun ElatApp() {
    val labels = listOf("خانه", "بازارها", "رادیو", "جستجو", "پروفایل")
    val icons = listOf(Icons.Default.Home, Icons.Default.ShowChart, Icons.Default.Mic, Icons.Default.Search, Icons.Default.Person)
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }

    selectedArticle?.let {
        DetailScreen(article = it, onBack = { selectedArticle = null })
        return
    }

    Scaffold(
        topBar = { BrandHeader() },
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icons[index], label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> FeedScreen(onOpen = { selectedArticle = it })
                1 -> MarketsScreen()
                2 -> RadioScreen()
                3 -> AskScreen()
                else -> ProfileScreen()
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        Modifier.fillMaxWidth().background(ElatBlue).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("ELAT.ir", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
        Text("خبرگزاری اقتصادی", color = Color.White.copy(alpha = .9f), fontSize = 13.sp)
    }
}

@Composable
private fun FeedScreen(
    onOpen: (NewsArticle) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("اختصاصی ELAT", "خبر فوری", "تحلیل ویژه", "بازارها").forEachIndexed { i, title ->
                    AssistChip(
                        onClick = {},
                        label = { Text(title) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (i == 0) ElatBlue2 else Color.White,
                            labelColor = if (i == 0) Color.White else ElatBlue
                        )
                    )
                }
            }
        }

        state.articles.firstOrNull { it.featured }?.let { article ->
            item {
                Card(
                    Modifier.fillMaxWidth().clickable { onOpen(article) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ElatBlue)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("اختصاصی ELAT", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(22.dp))
                        Text(article.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text(article.summary, color = Color.White.copy(alpha = .88f), fontSize = 15.sp)
                        Spacer(Modifier.height(14.dp))
                        Text("تحلیل اقتصادی • " + article.minutes.toString().toPersianDigits() + " دقیقه", color = Color(0xFFDFF5E7))
                    }
                }
            }
        }

        item { MarketStrip() }
        item { Text("آخرین اخبار اقتصادی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
        items(state.articles.filterNot { it.featured }) { article ->
            Card(
                Modifier.fillMaxWidth().clickable { onOpen(article) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(article.category, color = ElatBlue2, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(article.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(article.summary, color = Color(0xFF5E6770), fontSize = 14.sp)
                }
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun MarketStrip() {
    val quotes = listOf(
        "دلار" to "58,320",
        "طلا" to "4,145,000",
        "بورس" to "2,274,657",
        "بیت‌کوین" to "104,450"
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        quotes.forEachIndexed { index, pair ->
            Card(
                Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pair.first, fontSize = 12.sp)
                    Text(pair.second.toPersianDigits(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(if (index == 3) "−۰.۴٪" else "+۱.۲٪", color = if (index == 3) ElatRed else ElatGreen, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(article: NewsArticle, onBack: () -> Unit) {
    var layer by remember { mutableStateOf(ReadingLayer.QUICK) }
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFFF7F9FB)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Button(onClick = onBack) { Text("بازگشت") }
            Spacer(Modifier.height(10.dp))
            Text(article.category, color = ElatGreen, fontWeight = FontWeight.Bold)
            Text(article.title, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("خبرگزاری ELAT • امروز", color = Color.Gray)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadingLayer.entries.forEach { item ->
                    AssistChip(
                        onClick = { layer = item },
                        label = { Text(item.titleFa + " (" + item.subtitleFa + ")") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (layer == item) ElatBlue2 else Color.White,
                            labelColor = if (layer == item) Color.White else ElatBlue
                        )
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF7EE))) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        when (layer) {
                            ReadingLayer.QUICK -> "خلاصه هوش مصنوعی"
                            ReadingLayer.ANALYSIS -> "تحلیل اقتصادی"
                            ReadingLayer.DEEP -> "متن عمیق"
                        },
                        color = ElatGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    val body = when (layer) {
                        ReadingLayer.QUICK ->
                            "• مهم‌ترین داده خبر و تغییر اصلی بازار\n• اثر مستقیم بر سرمایه‌گذاران و کسب‌وکارها\n• محرک اصلی این روند اقتصادی\n• موضوعی که باید در روزهای آینده پیگیری شود"
                        ReadingLayer.ANALYSIS ->
                            "این لایه برای توضیح رابطه علت و معلولی، اثر بر صنایع، فرصت‌ها، ریسک‌ها و سناریوهای محتمل طراحی شده است."
                        ReadingLayer.DEEP ->
                            "نسخه عمیق متن کامل خبر، داده‌های زمینه‌ای، تحلیل‌های تکمیلی، منابع و اخبار مرتبط ELAT را نمایش می‌دهد."
                    }
                    Text(body, lineHeight = 27.sp)
                }
            }
        }
        item {
            Text("واکنش تحلیلی شما", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Reaction.entries.forEach { Text(it.emoji, fontSize = 30.sp, modifier = Modifier.padding(6.dp)) }
            }
        }
        item {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("ساخت پوستر خبری برای استوری")
            }
        }
    }
}

@Composable
private fun MarketsScreen() {
    val rows = listOf(
        "دلار آمریکا" to "58,320 تومان",
        "یورو" to "63,450 تومان",
        "طلای ۱۸ عیار" to "4,145,000 تومان",
        "سکه امامی" to "49,850,000 تومان",
        "شاخص کل بورس" to "2,274,657",
        "بیت‌کوین" to "104,450 دلار"
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("بازارها", fontSize = 28.sp, fontWeight = FontWeight.Black) }
        items(rows) { pair ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(pair.first, fontWeight = FontWeight.Bold)
                    Text(pair.second.toPersianDigits(), color = ElatGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RadioScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("رادیو تحلیل ELAT", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F3FB))) {
            Column(Modifier.padding(18.dp)) {
                Text("بولتن صبحگاهی", color = ElatBlue2, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("خلاصه وضعیت بازارهای جهانی، ارز و طلا، بورس تهران و چشم‌انداز اقتصادی روز")
                Spacer(Modifier.height(16.dp))
                Button(onClick = {}) { Text("▶ پخش بولتن") }
            }
        }
        Spacer(Modifier.height(14.dp))
        Card {
            Column(Modifier.padding(18.dp)) {
                Text("پادکست تحلیلی هفتگی", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("روایت عمیق‌تر از مهم‌ترین پرونده‌های اقتصادی هفته")
            }
        }
    }
}

@Composable
private fun AskScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("پرسش از ELAT", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("سؤال اقتصادی بپرسید؛ پاسخ نهایی فقط بر پایه محتوای elat.ir ساخته خواهد شد.")
        Spacer(Modifier.height(20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF7EE))) {
            Column(Modifier.padding(18.dp)) {
                Text("ELAT AI", color = ElatGreen, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("نمونه: چشم‌انداز قیمت طلا در ماه‌های آینده چیست؟")
                Spacer(Modifier.height(12.dp))
                Text("اتصال Firebase AI Logic + Gemini در اسپرینت بعدی فعال می‌شود.", color = Color(0xFF5F6972))
            }
        }
    }
}

@Composable
private fun ProfileScreen() {
    val rows = listOf("علاقه‌مندی‌ها", "اخبار ذخیره‌شده", "واکنش‌های من", "تنظیمات اعلان‌ها", "حالت شب", "درباره ELAT", "تماس با ما")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("پروفایل", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("کاربر مهمان", color = Color.Gray)
        }
        items(rows) { title ->
            Card(Modifier.fillMaxWidth()) { Text(title, Modifier.padding(18.dp), fontWeight = FontWeight.Medium) }
        }
    }
}

private fun String.toPersianDigits(): String {
    val fa = charArrayOf('۰','۱','۲','۳','۴','۵','۶','۷','۸','۹')
    return buildString {
        for (c in this@toPersianDigits) append(if (c in '0'..'9') fa[c - '0'] else c)
    }
}
