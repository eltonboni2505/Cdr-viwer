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
  private static final int BG=Color.rgb(5,9,8),SURFACE=Color.rgb(23,28,26),ACCENT=Color.rgb(80,235,0),TEXT=Color.WHITE,MUTED=Color.rgb(165,177,196);
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
  private LinearLayout toolbar,thumbnailStrip;
  private HorizontalScrollView thumbnailScroll;
  private android.widget.FrameLayout root;
  private LinearLayout home;
  private String currentFile="Arquivo CDR";
  private void toggleControls(){toolbar.setVisibility(toolbar.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);}
  private void openPicker(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);}
  private void fullscreen(){
    getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
    getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  }
  @Override public void onCreate(Bundle saved){super.onCreate(saved);
    fullscreen();
    root=new android.widget.FrameLayout(this);root.setBackgroundColor(Color.BLACK);
    preview=new ZoomImageView(this);root.addView(preview,new android.widget.FrameLayout.LayoutParams(-1,-1));
    toolbar=new LinearLayout(this);toolbar.setOrientation(1);toolbar.setPadding(dp(14),dp(10),dp(14),dp(14));toolbar.setBackgroundColor(0xE609100C);
    android.widget.FrameLayout.LayoutParams barParams=new android.widget.FrameLayout.LayoutParams(-1,-2,android.view.Gravity.BOTTOM);
    root.addView(toolbar,barParams);
    LinearLayout top=new LinearLayout(this);top.setGravity(android.view.Gravity.CENTER_VERTICAL);
    TextView title=label("Ver.cdr",19,TEXT,true);top.addView(title,new LinearLayout.LayoutParams(0,dp(44),1));
    Button update=action("↻",SURFACE);top.addView(update,new LinearLayout.LayoutParams(dp(50),dp(44)));
    toolbar.addView(top);
    status=label("Toque para ocultar os controles",12,MUTED,false);toolbar.addView(status);
    LinearLayout nav=new LinearLayout(this);nav.setGravity(android.view.Gravity.CENTER_VERTICAL);
    Button previous=action("‹",SURFACE);previous.setTextSize(24);nav.addView(previous,new LinearLayout.LayoutParams(dp(56),dp(48)));
    pageStatus=label("Nenhuma página",14,TEXT,true);pageStatus.setGravity(android.view.Gravity.CENTER);nav.addView(pageStatus,new LinearLayout.LayoutParams(0,dp(48),1));
    Button next=action("›",SURFACE);next.setTextSize(24);nav.addView(next,new LinearLayout.LayoutParams(dp(56),dp(48)));
    toolbar.addView(nav);
    thumbnailScroll=new HorizontalScrollView(this);thumbnailScroll.setHorizontalScrollBarEnabled(false);
    thumbnailStrip=new LinearLayout(this);thumbnailStrip.setPadding(0,dp(6),0,dp(6));
    thumbnailScroll.addView(thumbnailStrip);toolbar.addView(thumbnailScroll);
    LinearLayout actions=new LinearLayout(this);
    Button open=action("＋  Abrir CDR",ACCENT);Button share=action("↗  Compartilhar PNG",SURFACE);
    LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(0,dp(52),1);op.setMargins(0,dp(8),dp(6),0);actions.addView(open,op);
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(52),1);sp.setMargins(dp(6),dp(8),0,0);actions.addView(share,sp);
    toolbar.addView(actions);
    Button exportAll=action("↓  Exportar todas as páginas (ZIP)",SURFACE);
    LinearLayout.LayoutParams allParams=new LinearLayout.LayoutParams(-1,dp(44));allParams.setMargins(0,dp(8),0,0);
    toolbar.addView(exportAll,allParams);exportAll.setOnClickListener(v->exportAllPng());
    buildHome();
    setContentView(root);
    preview.setGalleryGestures(this::toggleControls,delta->showPage(pageIndex+delta));
    update.setOnClickListener(v->new UpdateManager(this).check(true));
    previous.setOnClickListener(v->showPage(pageIndex-1));next.setOnClickListener(v->showPage(pageIndex+1));
    open.setOnClickListener(v->openPicker());
    share.setOnClickListener(v->sharePng());
    Intent i=getIntent();if(i!=null){Uri u=i.getParcelableExtra(Intent.EXTRA_STREAM);if(u==null)u=i.getData();if(u!=null)load(u);}
    new UpdateManager(this).check(false);
  }
  private void buildHome(){
    home=new LinearLayout(this);
    home.setOrientation(1);
    home.setGravity(android.view.Gravity.CENTER);
    home.setPadding(dp(26),dp(20),dp(26),dp(20));
    home.setBackgroundColor(BG);

    // The launcher drawable is a temporary fallback until the approved PNG is added as a resource.
    ImageView brandIcon=new ImageView(this);
    brandIcon.setImageResource(getResources().getIdentifier("vercdr_icon","drawable",getPackageName()));
    brandIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
    LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(200),dp(200));
    iconParams.gravity=android.view.Gravity.CENTER;
    home.addView(brandIcon,iconParams);

    TextView brand=label("Ver.cdr",35,TEXT,true);
    brand.setGravity(android.view.Gravity.CENTER);
    home.addView(brand);
    TextView subtitle=label("VISUALIZADOR DE CDR",12,ACCENT,true);
    subtitle.setLetterSpacing(.22f);
    subtitle.setGravity(android.view.Gravity.CENTER);
    LinearLayout.LayoutParams subtitleParams=new LinearLayout.LayoutParams(-1,-2);
    subtitleParams.setMargins(0,dp(4),0,0);
    home.addView(subtitle,subtitleParams);

    TextView description=label("Abra e visualize seus arquivos do CorelDRAW no celular.",16,TEXT,false);
    description.setGravity(android.view.Gravity.CENTER);
    LinearLayout.LayoutParams descriptionParams=new LinearLayout.LayoutParams(-1,-2);
    descriptionParams.setMargins(0,dp(42),0,dp(28));
    home.addView(description,descriptionParams);

    Button open=action("▣   Abrir arquivo CDR",ACCENT);
    open.setTextColor(Color.BLACK);
    home.addView(open,new LinearLayout.LayoutParams(-1,dp(60)));
    Button updates=action("↻   Verificar atualizações",SURFACE);
    LinearLayout.LayoutParams updatesParams=new LinearLayout.LayoutParams(-1,dp(52));
    updatesParams.setMargins(0,dp(12),0,0);
    home.addView(updates,updatesParams);

    TextView hint=label("Deslize entre páginas  •  Amplie com dois dedos",12,MUTED,false);
    hint.setGravity(android.view.Gravity.CENTER);
    LinearLayout.LayoutParams hintParams=new LinearLayout.LayoutParams(-1,-2);
    hintParams.setMargins(0,dp(34),0,0);
    home.addView(hint,hintParams);
    root.addView(home,new android.widget.FrameLayout.LayoutParams(-1,-1));
    open.setOnClickListener(v->openPicker());
    updates.setOnClickListener(v->new UpdateManager(this).check(true));
  }
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==1&&result==RESULT_OK&&data!=null)load(data.getData());}
  private void load(Uri uri){try{
    String mime=getContentResolver().getType(uri);String name=uri.toString().toLowerCase(Locale.ROOT);
    try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst()){int idx=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(idx>=0)name=cursor.getString(idx).toLowerCase(Locale.ROOT);}}
    currentFile=name.substring(name.lastIndexOf("/") + 1);
    pages.clear();bitmap=null;thumbnailStrip.removeAllViews();
    if(name.endsWith(".cdr")||(mime!=null&&(mime.toLowerCase(Locale.ROOT).contains("coreldraw")||mime.toLowerCase(Locale.ROOT).contains("zip")))){
      pages.clear();pages.addAll(readCdrPreviews(uri));
      bitmap=pages.isEmpty()?null:pages.get(0);
      if(bitmap==null){status.setText("CDR sem prévias de página compatíveis. Renderização vetorial ainda indisponível.");pageStatus.setText("Nenhuma página");preview.setImageDrawable(null);return;}
      refreshThumbnails();showPage(0);
      status.setText("Prévia incorporada do CDR: "+bitmap.getWidth()+" × "+bitmap.getHeight()+" px. Pode estar desatualizada ou em baixa resolução.");
      return;
    }
    if("application/pdf".equals(mime)||name.endsWith(".pdf")){
      try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");PdfRenderer renderer=new PdfRenderer(fd)){
        int limit=Math.min(renderer.getPageCount(),80);
        for(int i=0;i<limit;i++){
          try(PdfRenderer.Page p=renderer.openPage(i)){
            float factor=Math.min(2f,2000f/Math.max(p.getWidth(),p.getHeight()));
            Bitmap page=Bitmap.createBitmap(Math.max(1,(int)(p.getWidth()*factor)),Math.max(1,(int)(p.getHeight()*factor)),Bitmap.Config.ARGB_8888);
            page.eraseColor(Color.WHITE);
            p.render(page,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pages.add(page);
          }
        }
      }
    }else{
      try(InputStream in=getContentResolver().openInputStream(uri)){
        bitmap=BitmapFactory.decodeStream(in);
        if(bitmap!=null)pages.add(bitmap);
      }
    }
    if(pages.isEmpty())throw new IOException("Formato não suportado nesta versão");
    refreshThumbnails();showPage(0);
    status.setText("Imagem disponível: exportação limitada à resolução carregada.");
  }catch(Exception e){bitmap=null;pages.clear();pageStatus.setText("Nenhuma página");preview.setImageDrawable(null);status.setText("Não foi possível abrir: "+e.getMessage());}}
  private void showPage(int index){
    if(index<0||index>=pages.size())return;
    pageIndex=index;bitmap=pages.get(index);preview.setImageBitmap(bitmap);
    pageStatus.setText("Página "+(index+1)+" de "+pages.size());
    highlightThumbnail();
    if(home!=null)home.setVisibility(View.GONE);
    toolbar.setVisibility(View.GONE);
  }
  private void refreshThumbnails(){
    thumbnailStrip.removeAllViews();
    for(int i=0;i<pages.size();i++){
      final int index=i;
      ImageView thumb=new ImageView(this);
      thumb.setImageBitmap(pages.get(i));
      thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
      thumb.setBackground(shape(SURFACE,8));
      thumb.setPadding(dp(4),dp(4),dp(4),dp(4));
      thumb.setTag(i);
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(62),dp(76));
      lp.setMargins(dp(3),0,dp(3),0);
      thumbnailStrip.addView(thumb,lp);
      thumb.setOnClickListener(v->showPage(index));
    }
    thumbnailScroll.setVisibility(pages.size()>1?View.VISIBLE:View.GONE);
  }
  private void highlightThumbnail(){
    for(int i=0;i<thumbnailStrip.getChildCount();i++){
      View child=thumbnailStrip.getChildAt(i);
      child.setBackground(shape(i==pageIndex?ACCENT:SURFACE,8));
    }
    if(thumbnailStrip.getChildCount()>pageIndex){
      View selected=thumbnailStrip.getChildAt(pageIndex);
      thumbnailScroll.post(()->thumbnailScroll.smoothScrollTo(Math.max(0,selected.getLeft()-dp(100)),0));
    }
  }
  private void exportAllPng(){
    if(pages.isEmpty()){Toast.makeText(this,"Abra um arquivo primeiro",Toast.LENGTH_SHORT).show();return;}
    try{
      File file=new File(getCacheDir(),"vercdr-paginas.zip");
      try(java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(new FileOutputStream(file))){
        for(int i=0;i<pages.size();i++){
          zip.putNextEntry(new ZipEntry(String.format(Locale.ROOT,"pagina-%03d.png",i+1)));
          if(!pages.get(i).compress(Bitmap.CompressFormat.PNG,100,zip))throw new IOException("Erro na página "+(i+1));
          zip.closeEntry();
        }
      }
      Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",file);
      Intent send=new Intent(Intent.ACTION_SEND);
      send.setType("application/zip");send.putExtra(Intent.EXTRA_STREAM,uri);
      send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivity(Intent.createChooser(send,"Compartilhar páginas PNG (ZIP)"));
    }catch(Exception e){Toast.makeText(this,"Falha ao exportar: "+e.getMessage(),Toast.LENGTH_LONG).show();}
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
