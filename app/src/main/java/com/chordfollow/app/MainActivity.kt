package com.chordfollow.app

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.net.Uri
import android.os.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.util.concurrent.Executors

data class Chord(val name:String, val full:Set<Int>, val simple:Set<Int>)
object Chords {
 private val roots=mapOf("C" to 0,"B#" to 0,"C#" to 1,"DB" to 1,"D" to 2,"D#" to 3,"EB" to 3,"E" to 4,"FB" to 4,"F" to 5,"E#" to 5,"F#" to 6,"GB" to 6,"G" to 7,"G#" to 8,"AB" to 8,"A" to 9,"A#" to 10,"BB" to 10,"B" to 11,"CB" to 11)
 private val types=mapOf("" to listOf(0,4,7),"maj" to listOf(0,4,7),"m" to listOf(0,3,7),"min" to listOf(0,3,7),"7" to listOf(0,4,7,10),"maj7" to listOf(0,4,7,11),"m7" to listOf(0,3,7,10),"dim" to listOf(0,3,6),"aug" to listOf(0,4,8),"sus2" to listOf(0,2,7),"sus4" to listOf(0,5,7),"6" to listOf(0,4,7,9),"m6" to listOf(0,3,7,9),"9" to listOf(0,4,7,10,2),"add9" to listOf(0,4,7,2))
 private val token=Regex("^[A-G](#|b)?(maj7|maj|min|m7|m6|m|dim|aug|sus2|sus4|add9|6|7|9)?(/[A-G](#|b)?)?$")
 fun parse(s:String):Chord? {
  val t=s.trim(); if(!token.matches(t)) return null
  val base=t.substringBefore("/")
  val n=if(base.length>1 && (base[1]=='#'||base[1]=='b')) 2 else 1
  val root=roots[base.substring(0,n).uppercase()] ?: return null
  val ints=types[base.substring(n).lowercase()] ?: return null
  val full=ints.map{(root+it)%12}.toSet()
  return Chord(t,full,setOf(root,(root+ints.getOrElse(1){4})%12,(root+ints.getOrElse(2){7})%12))
 }
 fun extract(text:String)=text.split(Regex("\\s+")).map{it.trim(',', '.', '|','(',')')}.mapNotNull{parse(it)}
}
class Follower(private val changed:(Chord?,Chord?)->Unit, private val done:()->Unit) {
 var simple=true; private var song=listOf<Chord>(); private var index=0; private val held=mutableSetOf<Int>(); private var armed=true
 fun load(c:List<Chord>){song=c;index=0;held.clear();armed=true;changed(song.getOrNull(0),song.getOrNull(1))}
 fun noteOn(n:Int){held.add(n);checkChord()}
 fun noteOff(n:Int){held.remove(n);checkChord()}
 private fun checkChord(){
  val c=song.getOrNull(index)?:return; val need=if(simple)c.simple else c.full
  if(!held.map{it%12}.toSet().containsAll(need)||held.isEmpty()){armed=true;return}
  if(!armed)return
  armed=false
  if(index==song.lastIndex)done() else {index++;changed(song[index],song.getOrNull(index+1))}
 }
}
class MainActivity:AppCompatActivity(), UsbMidi.Listener {
 private lateinit var usb:UsbMidi; private lateinit var follower:Follower
 private lateinit var current:TextView; private lateinit var next:TextView; private lateinit var status:TextView; private lateinit var mode:Button
 private val pdfRequest=1001
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(R.layout.activity_main)
  current=findViewById(R.id.currentChordView);next=findViewById(R.id.nextChordView);status=findViewById(R.id.statusView);mode=findViewById(R.id.modeButton)
  follower=Follower({a,b->current.text=a?.name?:"—";next.text=b?.name?:"סוף"}, {current.text="סיום!";next.text=""})
  findViewById<Button>(R.id.importPdfButton).setOnClickListener{val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/pdf"};startActivityForResult(i,pdfRequest)}
  findViewById<Button>(R.id.pasteTextButton).setOnClickListener{val e=EditText(this).apply{minLines=6;hint="הדבק כאן אקורדים"};AlertDialog.Builder(this).setTitle("ייבוא אקורדים").setView(e).setPositiveButton("ייבוא"){_,_->load(Chords.extract(e.text.toString()))}.setNegativeButton("ביטול",null).show()}
  mode.setOnClickListener{follower.simple=!follower.simple;mode.text=if(follower.simple)"מצב פשוט · Simple" else "מצב מלא · Full"}
  usb=UsbMidi(this,this)
 }
 override fun onResume(){super.onResume();usb.start()}
 override fun onPause(){usb.stop();super.onPause()}
 @Deprecated("Deprecated in Java") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==pdfRequest&&resultCode==Activity.RESULT_OK){try{val uri=data?.data?:return;PDFBoxResourceLoader.init(applicationContext);val text=contentResolver.openInputStream(uri)!!.use{PDDocument.load(it).use{doc->PDFTextStripper().getText(doc)}};load(Chords.extract(text))}catch(e:Exception){Toast.makeText(this,"שגיאה בקריאת PDF: ${e.message}",Toast.LENGTH_LONG).show()}}}
 private fun load(c:List<Chord>){if(c.isEmpty()){Toast.makeText(this,"לא זוהו אקורדים",Toast.LENGTH_LONG).show();return};follower.load(c);Toast.makeText(this,"נטענו ${c.size} אקורדים",Toast.LENGTH_SHORT).show()}
 override fun onNoteOn(note:Int)=follower.noteOn(note)
 override fun onNoteOff(note:Int)=follower.noteOff(note)
 override fun onConnected(name:String){status.text="מחובר: $name"}
 override fun onDisconnected(){status.text="המקלדת נותקה"}
 override fun onError(message:String){Toast.makeText(this,message,Toast.LENGTH_LONG).show()}
}
class UsbMidi(private val context:Context,private val listener:Listener) {
 interface Listener { fun onNoteOn(note:Int);fun onNoteOff(note:Int);fun onConnected(name:String);fun onDisconnected();fun onError(message:String) }
 private val action="com.chordfollow.app.USB_PERMISSION"
 private val manager=context.getSystemService(Context.USB_SERVICE) as UsbManager
 private val handler=Handler(Looper.getMainLooper());private val executor=Executors.newSingleThreadExecutor()
 @Volatile private var reading=false;private var conn:UsbDeviceConnection?=null;private var intf:UsbInterface?=null
 private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(i.action==action){val d=i.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE);if(i.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false)&&d!=null)open(d) else listener.onError("הרשאת USB נדחתה")}}}
 fun start(){
  try{ContextCompat.registerReceiver(context,receiver,IntentFilter(action),ContextCompat.RECEIVER_NOT_EXPORTED)}catch(_:Exception){}
  for(d in manager.deviceList.values){val midi=(0 until d.interfaceCount).any{d.getInterface(it).interfaceClass==UsbConstants.USB_CLASS_AUDIO&&d.getInterface(it).interfaceSubclass==3};if(midi){if(manager.hasPermission(d))open(d) else {val p=PendingIntent.getBroadcast(context,0,Intent(action),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE);manager.requestPermission(d,p)};return}}
 }
 fun stop(){reading=false;try{context.unregisterReceiver(receiver)}catch(_:Exception){};try{conn?.releaseInterface(intf)}catch(_:Exception){};conn?.close();conn=null}
 private fun open(d:UsbDevice){
  var chosen:UsbInterface?=null;var ep:UsbEndpoint?=null
  for(i in 0 until d.interfaceCount){val x=d.getInterface(i);if(x.interfaceClass==UsbConstants.USB_CLASS_AUDIO&&x.interfaceSubclass==3)for(j in 0 until x.endpointCount){val e=x.getEndpoint(j);if(e.direction==UsbConstants.USB_DIR_IN&&e.type==UsbConstants.USB_ENDPOINT_XFER_BULK){chosen=x;ep=e}}}
  if(chosen==null||ep==null){listener.onError("לא נמצאה כניסת MIDI");return}
  val c=manager.openDevice(d)?:run{listener.onError("לא ניתן לפתוח התקן USB");return}
  if(!c.claimInterface(chosen,true)){c.close();listener.onError("לא ניתן להתחבר לממשק MIDI");return}
  conn=c;intf=chosen;reading=true;handler.post{listener.onConnected(d.productName?:"MIDI")}
  executor.execute{val b=ByteArray(64);while(reading){val n=c.bulkTransfer(ep,b,b.size,200);if(n>0){var p=0;while(p+3<n){val s=b[p+1].toInt() and 255;val note=b[p+2].toInt() and 255;val vel=b[p+3].toInt() and 255;p+=4;when(s and 0xF0){0x90->if(vel>0)handler.post{listener.onNoteOn(note)}else handler.post{listener.onNoteOff(note)};0x80->handler.post{listener.onNoteOff(note)}}}}};handler.post{listener.onDisconnected()}}
 }
}
