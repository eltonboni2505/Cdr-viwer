package com.ideia.cdrviewer;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.Locale;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import androidx.core.content.FileProvider;

public class MainActivity extends Activity {
  private ZoomImageView preview; private TextView status,pageStatus; private Bitmap bitmap;
  private final java.util.ArrayList<Bitmap> pages=new java.util.ArrayList<>();private int pageIndex=0;
  private static final int BG=Color.rgb(12,17,29),SURFACE=Color.rgb(25,33,49),ACCENT=Color.rgb(71,139,255),TEXT=Color.WHITE,MUTED=Color.rgb(165,177,196);
  private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  private android.graphics.drawable.GradientDrawable shape(int color,int radius){
    android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
  }
  private TextView label(String s,int size,int color,boolean bold){
    TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);return t;
  }
  private Button action(String s,int color){
    Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setAllCaps(false);b.setTextSize(13);b.setBackground(shape(color,14));b.setPadding(dp(8),0,dp(8),0);return b;
  }
  private LinearLayout toolbar;
  private android.widget.FrameLayout root;
  private LinearLayout home;
  private String currentFile="Arquivo CDR";
  private void toggleControls(){toolbar.setVisibility(toolbar.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);}
  private void fullscreen(){
    getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
    getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  }
  @Override public void onCreate(Bundle saved){super.onCreate(saved);
    fullscreen();
    root=new android.widget.FrameLayout(this);root.setBackgroundColor(Color.BLACK);
    preview=new ZoomImageView(this);root.addView(preview,new android.widget.FrameLayout.LayoutParams(-1,-1));
    toolbar=new LinearLayout(this);toolbar.setOrientation(1);toolbar.setPadding(dp(14),dp(18),dp(14),dp(18));toolbar.setBackgroundColor(0xD9121926);
    android.widget.FrameLayout.LayoutParams barParams=new android.widget.FrameLayout.LayoutParams(-1,-2,android.view.Gravity.BOTTOM);
    root.addView(toolbar,barParams);
    LinearLayout top=new LinearLayout(this);top.setGravity(android.view.Gravity.CENTER_VERTICAL);
    TextView title=label("CDR Viewer",19,TEXT,true);top.addView(title,new LinearLayout.LayoutParams(0,dp(44),1));
    Button update=action("↻",SURFACE);top.addView(update,new LinearLayout.LayoutParams(dp(50),dp(44)));
    toolbar.addView(top);
    status=label("Toque para ocultar os controles",12,MUTED,false);toolbar.addView(status);
    LinearLayout nav=new LinearLayout(this);nav.setGravity(android.view.Gravity.CENTER_VERTICAL);
    Button previous=action("‹",SURFACE);previous.setTextSize(24);nav.addView(previous,new LinearLayout.LayoutParams(dp(56),dp(48)));
    pageStatus=label("Nenhuma página",14,TEXT,true);pageStatus.setGravity(android.view.Gravity.CENTER);nav.addView(pageStatus,new LinearLayout.LayoutParams(0,dp(48),1));
    Button next=action("›",SURFACE);next.setTextSize(24);nav.addView(next,new LinearLayout.LayoutParams(dp(56),dp(48)));
    toolbar.addView(nav);
    LinearLayout actions=new LinearLayout(this);
    Button open=action("＋  Abrir",ACCENT);Button share=action("↗  PNG",SURFACE);
    LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(0,dp(52),1);op.setMargins(0,dp(8),dp(6),0);actions.addView(open,op);
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(52),1);sp.setMargins(dp(6),dp(8),0,0);actions.addView(share,sp);
    toolbar.addView(actions);
    buildHome();
    setContentView(root);
    preview.setGalleryGestures(this::toggleControls,delta->showPage(pageIndex+delta));
    update.setOnClickListener(v->new UpdateManager(this).check(true));
    previous.setOnClickListener(v->showPage(pageIndex-1));next.setOnClickListener(v->showPage(pageIndex+1));
    open.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);});
    share.setOnClickListener(v->sharePng());
    Intent i=getIntent();if(i!=null){Uri u=i.getParcelableExtra(Intent.EXTRA_STREAM);if(u==null)u=i.getData();if(u!=null)load(u);}
    new UpdateManager(this).check(false);
  }
  private void buildHome(){
    home=new LinearLayout(this);home.setOrientation(1);home.setGravity(android.view.Gravity.CENTER);
    home.setPadding(dp(28),dp(24),dp(28),dp(24));home.setBackgroundColor(BG);
    TextView mark=label("◉",70,0xFF84F600,true);mark.setGravity(android.view.Gravity.CENTER);home.addView(mark);
    TextView brand=label("Ver.cdr",35,TEXT,true);brand.setGravity(android.view.Gravity.CENTER);home.addView(brand);
    TextView subtitle=label("Visualizador de arquivos CDR",14,MUTED,false);subtitle.setGravity(android.view.Gravity.CENTER);home.addView(subtitle);
    Button open=action("▣   Abrir arquivo CDR",0xFF168B43);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(66));lp.setMargins(0,dp(50),0,dp(14));home.addView(open,lp);
    Button updates=action("↻   Verificar atualizações",SURFACE);home.addView(updates,new LinearLayout.LayoutParams(-1,dp(52)));
    TextView hint=label("Offline  •  Zoom  •  Compartilhar PNG",12,MUTED,false);hint.setGravity(android.view.Gravity.CENTER);
    LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,dp(24),0,0);home.addView(hint,hp);
    root.addView(home,new android.widget.FrameLayout.LayoutParams(-1,-1));
    open.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);});
    updates.setOnClickListener(v->new UpdateManager(this).check(true));
  }
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==1&&result==RESULT_OK&&data!=null)load(data.getData());}
  private void load(Uri uri){try{
    String mime=getContentResolver().getType(uri);String name=uri.toString().toLowerCase(Locale.ROOT);
    try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst()){int idx=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(idx>=0)name=cursor.getString(idx).toLowerCase(Locale.ROOT);}}
    currentFile=name.substring(name.lastIndexOf("/") + 1);
    if(name.endsWith(".cdr")||(mime!=null&&(mime.toLowerCase(Locale.ROOT).contains("coreldraw")||mime.toLowerCase(Locale.ROOT).contains("zip")))){
      pages.clear();pages.addAll(readCdrPreviews(uri));
      bitmap=pages.isEmpty()?null:pages.get(0);
      if(bitmap==null){status.setText("CDR reconhecido, mas não foi encontrada prévia PNG compatível. Renderização vetorial ainda indisponível.");preview.setImageDrawable(null);return;}
      showPage(0);
      status.setText("Prévia incorporada do CDR: "+bitmap.getWidth()+" × "+bitmap.getHeight()+" px. Pode estar desatualizada ou em baixa resolução.");
      return;
    }
    if("application/pdf".equals(mime)||name.endsWith(".pdf")){
      try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");PdfRenderer r=new PdfRenderer(fd)){
        PdfRenderer.Page p=r.openPage(0);float scale=Math.min(2f,2000f/Math.max(p.getWidth(),p.getHeight()));bitmap=Bitmap.createBitmap(Math.max(1,(int)(p.getWidth()*scale)),Math.max(1,(int)(p.getHeight()*scale)),Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);p.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);p.close();}
    }else{try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in);}}
    if(bitmap==null)throw new IOException("Formato não suportado nesta versão");pages.clear();pages.add(bitmap);showPage(0);status.setText("Prévia pronta. Amplie com dois dedos ou compartilhe o PNG.");
  }catch(Exception e){bitmap=null;pages.clear();pageStatus.setText("Nenhuma página");preview.setImageDrawable(null);status.setText("Não foi possível abrir: "+e.getMessage());}}
  private void showPage(int index){
    if(index<0||index>=pages.size())return;
    pageIndex=index;bitmap=pages.get(index);preview.setImageBitmap(bitmap);
    pageStatus.setText("Página "+(index+1)+" de "+pages.size());
    if(home!=null)home.setVisibility(View.GONE);
    toolbar.setVisibility(View.GONE);
  }
  private java.util.ArrayList<Bitmap> readCdrPreviews(Uri uri) throws IOException {
    java.util.TreeMap<Integer,Bitmap> numbered=new java.util.TreeMap<>();
    try(InputStream raw=getContentResolver().openInputStream(uri);ZipInputStream zip=new ZipInputStream(new BufferedInputStream(raw))){
      ZipEntry entry;
      while((entry=zip.getNextEntry())!=null){
        String n=entry.getName().toLowerCase(Locale.ROOT);
        java.util.regex.Matcher pageMatch=java.util.regex.Pattern.compile("^previews/page([1-9][0-9]*)\\.png$").matcher(n);
        if(!entry.isDirectory()&&pageMatch.matches()){
          ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buf=new byte[8192];int count;boolean oversized=false;
          while((count=zip.read(buf))!=-1){if(bytes.size()+count>16_000_000){oversized=true;break;}bytes.write(buf,0,count);}
          if(!oversized){byte[] data=bytes.toByteArray();Bitmap candidate=BitmapFactory.decodeByteArray(data,0,data.length);if(candidate!=null)numbered.put(Integer.parseInt(pageMatch.group(1)),candidate);}
        }
        zip.closeEntry();
      }
    }
    return new java.util.ArrayList<>(numbered.values());
  }
  private void sharePng(){if(bitmap==null){Toast.makeText(this,"Abra primeiro uma imagem ou PDF compatível",Toast.LENGTH_LONG).show();return;}
    try{File f=new File(getCacheDir(),"arte-compartilhada.png");try(FileOutputStream out=new FileOutputStream(f)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
      Uri u=FileProvider.getUriForFile(this,getPackageName()+".files",f);Intent send=new Intent(Intent.ACTION_SEND);send.setType("image/png");send.putExtra(Intent.EXTRA_STREAM,u);send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(send,"Compartilhar PNG"));
    }catch(Exception e){status.setText("Erro ao compartilhar: "+e.getMessage());}}
}
