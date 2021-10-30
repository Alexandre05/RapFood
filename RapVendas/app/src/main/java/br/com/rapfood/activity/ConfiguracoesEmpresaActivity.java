package br.com.rapfood.activity;

import static br.com.rapfood.helper.ConfiguracaoFirebase.getFirebaseStorage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.provider.PicassoProvider;


import java.io.ByteArrayOutputStream;
import java.security.Provider;
import java.util.Objects;

import br.com.rapfood.R;
import br.com.rapfood.helper.ConfiguracaoFirebase;
import br.com.rapfood.helper.UsuarioFirebase;
import br.com.rapfood.model.Empresa;

public class ConfiguracoesEmpresaActivity extends AppCompatActivity {

    private EditText editEmpresaNome, editEmpresaCategoria,
            editEmpresaTempo, editEmpresaTaxa;
    private ImageView imagePerfilEmpresa;

    private static final int SELECAO_GALERIA = 200;
    private StorageReference storageReference;
    private DatabaseReference firebaseRef;
    private String idUsuarioLogado;
    private String urlImagemSelecionada = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes_empresa);

        //Configurações iniciais
        inicializarComponentes();
        storageReference = getFirebaseStorage();
        firebaseRef = ConfiguracaoFirebase.getFirebase();
        idUsuarioLogado = UsuarioFirebase.getIdUsuario();

        //Configurações Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Configurações");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imagePerfilEmpresa.setOnClickListener(v -> {
            Intent i = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );
            if( i.resolveActivity(getPackageManager()) != null ){
                startActivityForResult(i, SELECAO_GALERIA);
            }
        });

        /*Recuperar dados da empresa*/
        recuperarDadosEmpresa();


    }

    private void recuperarDadosEmpresa(){

        DatabaseReference empresaRef = firebaseRef
                .child("empresas")
                .child( idUsuarioLogado );

        empresaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if( dataSnapshot.getValue() != null ){
                    Empresa empresas = dataSnapshot.getValue(Empresa.class);
                    editEmpresaNome.setText(empresas.getNome());
                    editEmpresaCategoria.setText(empresas.getCategoria());
                    editEmpresaTaxa.setText(empresas.getPrecoEntrega().toString());
                    editEmpresaTempo.setText(empresas.getTempo());

                    urlImagemSelecionada = empresas.getUrlImagem();
                    if( urlImagemSelecionada!=("")){
                        PicassoProvider.get().load(urlImagemSelecionada)
                                .into(imagePerfilEmpresa);
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void validarDadosEmpresa(View view){

        //Valida se os campos foram preenchidos
        String nome = editEmpresaNome.getText().toString();
        String taxa = editEmpresaTaxa.getText().toString();
        String categoria = editEmpresaCategoria.getText().toString();
        String tempo = editEmpresaTempo.getText().toString();

        if( !nome.isEmpty()){
            if( !taxa.isEmpty()){
                if( !categoria.isEmpty()){
                    if( !tempo.isEmpty()){

                        Empresa empresa = new Empresa();
                        empresa.setIdUsuario( idUsuarioLogado );
                        empresa.setNome( nome );
                        empresa.setPrecoEntrega( Double.parseDouble(taxa) );
                        empresa.setCategoria(categoria);
                        empresa.setTempo( tempo );
                        empresa.setUrlImagem( urlImagemSelecionada );
                        empresa.salvar();
                        finish();

                    }else{
                        exibirMensagem("Digite um tempo de entrega");
                    }
                }else{
                    exibirMensagem("Digite uma categoria");
                }
            }else{
                exibirMensagem("Digite uma taxa de entrega");
            }
        }else{
            exibirMensagem("Digite um nome para a empresa");
        }

    }

    private void exibirMensagem(String texto){
        Toast.makeText(this, texto, Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( resultCode == RESULT_OK){
            Bitmap imagem = null;

            try {

                switch (requestCode) {
                    case SELECAO_GALERIA:
                        Uri localImagem = data.getData();
                        imagem = MediaStore.Images
                                .Media
                                .getBitmap(
                                        getContentResolver(),
                                        localImagem
                                );
                        break;
                }

                if( imagem != null){

                    imagePerfilEmpresa.setImageBitmap( imagem );

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] dadosImagem = baos.toByteArray();

                    final StorageReference imagemRef = storageReference.child("imagens")
                            .child("empresas")
                            .child(idUsuarioLogado + "jpeg");

                    UploadTask uploadTask = imagemRef.putBytes( dadosImagem );
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ConfiguracoesEmpresaActivity.this,
                                    "Erro ao fazer upload da imagem",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener(ConfiguracoesEmpresaActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {

                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            imagemRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {

                                    Log.i("URI FIREBASE", uri.toString());

                                    Toast.makeText(ConfiguracoesEmpresaActivity.this,
                                            "URL: " + uri.toString(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });

                        }
                    });

                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }

    }

    private void inicializarComponentes(){
        editEmpresaNome = findViewById(R.id.editEmpresaNome);
        editEmpresaCategoria = findViewById(R.id.editEmpresaCategoria);
        editEmpresaTaxa = findViewById(R.id.editEmpresaTaxa);
        editEmpresaTempo = findViewById(R.id.editEmpresaTempo);
        imagePerfilEmpresa = findViewById(R.id.imagePerfilEmpresa);
    }

}
