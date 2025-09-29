package com.example.mycompaniesapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue

data class Company(
    val id: Int = 0,
    val title: String = "",
    val city: String = "",
    val webpage: String = "",
    val logoUrl: String? = null,
    val phone: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        setContent {
            MyCompaniesTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.testTag("MainActivitySurface") // Add test tag
                ) {
                    VerticalHorizontalScroll()
                }
            }
        }
    }
}

@Composable
fun VerticalHorizontalScroll() {
    val context = LocalContext.current
    var companies by remember { mutableStateOf(listOf<Company>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val db = Firebase.database.reference.child("companies")

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("Data changed: ${snapshot.value}")
                val companyList = mutableListOf<Company>()
                for (childSnapshot in snapshot.children) {
                    val company = childSnapshot.getValue<Company>()
                    println("Child data: ${childSnapshot.key} -> $company")
                    company?.let { companyList.add(it) }
                }
                companies = companyList.sortedBy { it.id }
                loading = false
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Database error: ${databaseError.message}")
                error = databaseError.message
                loading = false
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Purple500),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "My Companies App",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (loading) {
            Box(Modifier.fillMaxSize().testTag("loadingIndicator"), Alignment.Center) { CircularProgressIndicator() }
        } else if (error != null) {
            Box(Modifier.fillMaxSize().testTag("errorText"), Alignment.Center) { Text("Error: $error", color = Color.Red) }
        } else {
            LazyColumn {
                item {
                    Text(
                        text = "Recent List",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                item {
                    LazyRow {
                        items(companies.take(5)) { company ->
                            SmallCompanyCard(company)
                        }
                    }
                }

                item {
                    Text(
                        text = "Lists",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                items(companies) { company ->
                    LargeCompanyCard(company, context)
                }
            }
        }
    }
}

@Composable
fun SmallCompanyCard(company: Company) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(120.dp)
            .padding(10.dp, 5.dp, 5.dp, 0.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CompanyLogo(company.logoUrl)
            Spacer(modifier = Modifier.padding(5.dp))
            Text(
                text = company.title,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun LargeCompanyCard(company: Company, context: android.content.Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp, 5.dp, 10.dp, 5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompanyLogo(company.logoUrl)
                Spacer(modifier = Modifier.padding(5.dp))
                Column {
                    Text(
                        text = company.title,
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text(
                        text = "${company.city} - Phone: ${company.phone ?: "N/A"}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = company.webpage,
                        color = Color.Blue,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, company.webpage.toUri()))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompanyLogo(logoUrl: String?) {
    if (logoUrl != null) {
        AsyncImage(
            model = logoUrl,
            contentDescription = "Company Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            placeholder = painterResource(id = R.drawable.my_companies_icon)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.my_companies_icon),
            contentDescription = "Placeholder",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
        )
    }
}

val Purple500 = Color(0xFF6200EE)

@Composable
fun MyCompaniesTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}