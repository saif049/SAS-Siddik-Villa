package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.VillaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    currentUser: User,
    viewModel: VillaViewModel
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Chat, 1: Notices, 2: Queries, 3: Polls, 4: Documents

    val canPostOfficial = currentUser.role == UserRole.SYSTEM_ADMIN ||
            currentUser.role == UserRole.GOVERNANCE_MEMBER ||
            currentUser.role == UserRole.DELEGATED_ADMIN

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Chat") },
                icon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_community_chat")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Notices") },
                icon = { Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_community_notices")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Queries") },
                icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_community_queries")
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Polls") },
                icon = { Icon(Icons.Default.Poll, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_community_polls")
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("Docs") },
                icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_community_docs")
            )
        }

        when (selectedTab) {
            0 -> ChatTabContent(currentUser = currentUser, viewModel = viewModel)
            1 -> NoticesTabContent(currentUser = currentUser, viewModel = viewModel, canPost = canPostOfficial)
            2 -> QueriesTabContent(currentUser = currentUser, viewModel = viewModel)
            3 -> PollsTabContent(currentUser = currentUser, viewModel = viewModel, canCreatePoll = canPostOfficial)
            4 -> DocumentsTabContent(currentUser = currentUser, viewModel = viewModel, canUpload = canPostOfficial)
        }
    }
}

@Composable
fun ChatTabContent(currentUser: User, viewModel: VillaViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMe = msg.senderUserId == currentUser.userId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (!isMe) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = msg.senderName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Text(
                                            text = msg.senderRole,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(
                                text = msg.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(msg.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }

        // Chat Input Row
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Write message to community...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendChatMessage(currentUser, messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("send_chat_button")
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun NoticesTabContent(currentUser: User, viewModel: VillaViewModel, canPost: Boolean) {
    val announcements by viewModel.announcements.collectAsState()
    var showPostDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(announcements, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isPinned)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.isPinned) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            StatusBadge(status = item.category)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.content, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Published by ${item.publishedBy} • ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(item.publishedAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        if (canPost) {
            FloatingActionButton(
                onClick = { showPostDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("post_notice_fab"),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post Notice", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    if (showPostDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Meeting") }
        var isPinned by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPostDialog = false },
            title = { Text("Publish Notice / Circular") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Announcement Details") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                        Text("Pin to top")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            viewModel.postAnnouncement(title, content, category, isPinned, currentUser.fullName)
                            showPostDialog = false
                        }
                    }
                ) {
                    Text("Publish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun QueriesTabContent(currentUser: User, viewModel: VillaViewModel) {
    val allQueries by viewModel.queries.collectAsState()
    var showNewQueryDialog by remember { mutableStateOf(false) }

    val userQueries = if (currentUser.role == UserRole.SYSTEM_ADMIN || currentUser.role == UserRole.GOVERNANCE_MEMBER) {
        allQueries
    } else {
        allQueries.filter { it.userId == currentUser.userId }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (userQueries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No queries found. Submit an issue using the + button.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(userQueries, key = { it.id }) { q ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(q.subject, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Flat ${q.flatId} • By ${q.submitterName} • ${q.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                StatusBadge(status = q.status)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(q.description, style = MaterialTheme.typography.bodyMedium)

                            if (q.adminResponse != null) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Admin Response:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(q.adminResponse ?: "", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            // Quick resolve button for Admin
                            if ((currentUser.role == UserRole.SYSTEM_ADMIN || currentUser.role == UserRole.GOVERNANCE_MEMBER) && q.status != "RESOLVED") {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.respondToQuery(q.id, "Issue addressed and resolved by committee.", "RESOLVED", currentUser.fullName) },
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Mark Resolved", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showNewQueryDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("submit_query_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Submit Query", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    if (showNewQueryDialog) {
        var subject by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Plumbing") }

        AlertDialog(
            onDismissRequest = { showNewQueryDialog = false },
            title = { Text("Submit Member Query / Issue") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        placeholder = { Text("e.g. Water leak in balcony pipe") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subject.isNotBlank() && description.isNotBlank()) {
                            viewModel.submitQuery(currentUser, subject, description, category)
                            showNewQueryDialog = false
                        }
                    }
                ) {
                    Text("Submit Query")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewQueryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PollsTabContent(currentUser: User, viewModel: VillaViewModel, canCreatePoll: Boolean) {
    val polls by viewModel.polls.collectAsState()
    var showCreatePollDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(polls, key = { it.id }) { poll ->
                val options = poll.optionsList.split("|")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Community Poll", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            StatusBadge(status = if (poll.isActive) "ACTIVE" else "CLOSED")
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(poll.question, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (poll.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(poll.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(Modifier.height(14.dp))

                        // Options list
                        options.forEachIndexed { index, option ->
                            OutlinedButton(
                                onClick = { viewModel.votePoll(poll.id, currentUser, index) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(option, style = MaterialTheme.typography.bodyMedium)
                                    Icon(Icons.Default.HowToVote, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (canCreatePoll) {
            FloatingActionButton(
                onClick = { showCreatePollDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Poll", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    if (showCreatePollDialog) {
        var question by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var options by remember { mutableStateOf("Agree|Disagree|Need Discussion") }

        AlertDialog(
            onDismissRequest = { showCreatePollDialog = false },
            title = { Text("Create Community Poll") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Poll Question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Background / Cost Context") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = options,
                        onValueChange = { options = it },
                        label = { Text("Options (separated by |)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (question.isNotBlank()) {
                            viewModel.createPoll(question, description, options, currentUser.userId)
                            showCreatePollDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePollDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DocumentsTabContent(currentUser: User, viewModel: VillaViewModel, canUpload: Boolean) {
    val documents by viewModel.documents.collectAsState()

    // Role-based document access rule: OWNERS_ONLY docs hidden from Renters/Guard
    val visibleDocs = documents.filter { doc ->
        when (doc.allowedRoles) {
            "ADMIN_ONLY" -> currentUser.role == UserRole.SYSTEM_ADMIN || currentUser.role == UserRole.GOVERNANCE_MEMBER
            "OWNERS_ONLY" -> currentUser.role == UserRole.SYSTEM_ADMIN || currentUser.role == UserRole.GOVERNANCE_MEMBER || currentUser.role == UserRole.FLAT_OWNER
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(visibleDocs, key = { it.id }) { doc ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(doc.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("${doc.documentType} • Ref: ${doc.fileReference}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            if (doc.description.isNotBlank()) {
                                Text(doc.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.showToast("Opening document ${doc.fileReference}...") }) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                }
            }
        }
    }
}
