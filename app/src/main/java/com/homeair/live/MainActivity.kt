package com.homeair.live

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Channel(val number:Int,val name:String,val url:String,val group:String="Live")
private val demoChannels=listOf(
 Channel(1,"Home Air Live Demo","https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
 Channel(2,"News Demo","https://test-streams.mux.dev/test_001/stream.m3u8"),
 Channel(3,"Sports Demo","https://test-streams.mux.dev/bbb-abr/bbb-abr.m3u8")
)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);enableEdgeToEdge();setContent{HomeAirLiveApp()}}
}
@Composable fun HomeAirLiveApp(){
 var mode by remember{mutableStateOf<String?>(null)};var selected by remember{mutableIntStateOf(0)}
 if(mode==null)ModeChooser{mode=it}else if(mode=="TV")TvScreen(demoChannels,selected){selected=it}else MobileScreen(demoChannels,selected){selected=it}
}
@Composable fun BrandLogo(modifier:Modifier=Modifier){Box(modifier.background(Color(0xFFFF7A00),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Text("▶",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)}}
@Composable fun ModeChooser(onChoose:(String)->Unit){
 Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFFF7A00),Color.White)))){
  Column(Modifier.align(Alignment.Center).padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally){
   BrandLogo(Modifier.size(72.dp));Spacer(Modifier.height(18.dp));Text("Home Air Live",fontSize=34.sp,fontWeight=FontWeight.Bold,color=Color(0xFF17171A));Text("Choose your viewing mode",color=Color.DarkGray);Spacer(Modifier.height(28.dp))
   Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){ModeCard("TV MODE","Full-screen live TV",onChoose);ModeCard("MOBILE MODE","Dashboard & touch",onChoose)}
  }
 }
}
@Composable fun ModeCard(title:String,subtitle:String,onChoose:(String)->Unit){
 val key=if(title.startsWith("TV"))"TV" else "MOBILE"
 Card(Modifier.width(260.dp).height(150.dp).clickable{onChoose(key)}.focusable()){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.Center){Text(title,fontWeight=FontWeight.Bold,fontSize=21.sp,color=Color(0xFFF45100));Spacer(Modifier.height(8.dp));Text(subtitle,color=Color.DarkGray)}}
}
@Composable fun TvScreen(channels:List<Channel>,current:Int,onSelect:(Int)->Unit){
 var sidebar by remember{mutableStateOf(false)};var number by remember{mutableStateOf("")};val focus=remember{FocusRequester()}
 Box(Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent{
  if(it.type!=androidx.compose.ui.input.key.KeyEventType.KeyDown)return@onPreviewKeyEvent false
  when(it.nativeKeyEvent.keyCode){
   KeyEvent.KEYCODE_DPAD_LEFT->{sidebar=true;true};KeyEvent.KEYCODE_BACK->{if(sidebar){sidebar=false;true}else false}
   KeyEvent.KEYCODE_PAGE_UP,KeyEvent.KEYCODE_CHANNEL_UP,KeyEvent.KEYCODE_DPAD_UP->{onSelect((current-1+channels.size)%channels.size);true}
   KeyEvent.KEYCODE_PAGE_DOWN,KeyEvent.KEYCODE_CHANNEL_DOWN,KeyEvent.KEYCODE_DPAD_DOWN->{onSelect((current+1)%channels.size);true}
   KeyEvent.KEYCODE_0,KeyEvent.KEYCODE_1,KeyEvent.KEYCODE_2,KeyEvent.KEYCODE_3,KeyEvent.KEYCODE_4,KeyEvent.KEYCODE_5,KeyEvent.KEYCODE_6,KeyEvent.KEYCODE_7,KeyEvent.KEYCODE_8,KeyEvent.KEYCODE_9->{number+=(it.nativeKeyEvent.keyCode-KeyEvent.KEYCODE_0).toString();true}
   KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_DPAD_CENTER->{val n=number.toIntOrNull();val idx=channels.indexOfFirst{it.number==n};if(idx>=0)onSelect(idx);number="";true};else->false
  }
 }.focusRequester(focus).focusable()){
  Text("LIVE  "+channels[current].name,color=Color.White,fontSize=20.sp,modifier=Modifier.align(Alignment.TopStart).padding(24.dp));Text(channels[current].name,color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.BottomStart).padding(28.dp))
  if(number.isNotEmpty())Text(number,color=Color.White,fontSize=42.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.Center));if(sidebar)ChannelSidebar(channels,current){onSelect(it);sidebar=false}
 }
 LaunchedEffect(Unit){focus.requestFocus()}
}
@Composable fun ChannelSidebar(channels:List<Channel>,current:Int,onSelect:(Int)->Unit){
 Surface(Modifier.fillMaxHeight().width(380.dp),color=Color(0xF218181A)){Column(Modifier.padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){BrandLogo(Modifier.size(48.dp));Spacer(Modifier.width(12.dp));Text("Channels",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(16.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){items(channels){c->val i=channels.indexOf(c);Row(Modifier.fillMaxWidth().background(if(i==current)Color(0xFFFF7A00)else Color.Transparent,RoundedCornerShape(10.dp)).clickable{onSelect(i)}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text(c.number.toString(),color=Color.White,fontWeight=FontWeight.Bold,modifier=Modifier.width(45.dp));Text(c.name,color=Color.White,fontSize=18.sp)}}}}}
}
@Composable fun MobileScreen(channels:List<Channel>,current:Int,onSelect:(Int)->Unit){
 var showPlayer by remember{mutableStateOf(false)}
 if(showPlayer)Box(Modifier.fillMaxSize().background(Color.Black)){Text("LIVE  "+channels[current].name,color=Color.White,fontSize=22.sp,modifier=Modifier.align(Alignment.TopStart).padding(20.dp));Text("Swipe up/down to change channel",color=Color.White,modifier=Modifier.align(Alignment.BottomCenter).padding(24.dp))}
 else Column(Modifier.fillMaxSize().background(Color(0xFF0E0E10)).padding(20.dp)){Row(verticalAlignment=Alignment.CenterVertically){BrandLogo(Modifier.size(52.dp));Spacer(Modifier.width(12.dp));Text("Home Air Live",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(24.dp));Text("Live TV",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(channels){c->Card(Modifier.fillMaxWidth().clickable{onSelect(channels.indexOf(c));showPlayer=true}){Text(c.number.toString()+"  "+c.name,Modifier.padding(20.dp),fontSize=18.sp)}}}}
}
