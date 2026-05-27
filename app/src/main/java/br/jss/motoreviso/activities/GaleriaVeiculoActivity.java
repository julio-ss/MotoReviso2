package br.jss.motoreviso.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import br.jss.motoreviso.R;
import br.jss.motoreviso.adapters.ImagemVeiculoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.ImagemVeiculo;
import br.jss.motoreviso.utils.SystemBarHelper;

import java.util.ArrayList;
import java.util.List;

public class GaleriaVeiculoActivity extends AppCompatActivity {
    private static final String TAG = "GaleriaVeiculoActivity";

    private String veiculoId;
    private RecyclerView recyclerImagens;
    private ProgressBar progressBar;
    private TextView textVazio;
    private FloatingActionButton fabAdicionar;
    private FirebaseManager firebaseManager;
    private List<ImagemVeiculo> imagens;
    private ImagemVeiculoAdapter adapter;
    private ActivityResultLauncher<Intent> imagemLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galeria_veiculo);

        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        veiculoId = getIntent().getStringExtra("VEICULO_ID");
        if (veiculoId == null || veiculoId.isEmpty()) {
            Log.e(TAG, "VEICULO_ID não fornecido");
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();
        imagens = new ArrayList<>();

        recyclerImagens = findViewById(R.id.recycler_imagens);
        progressBar = findViewById(R.id.progress_bar);
        textVazio = findViewById(R.id.text_vazio);
        fabAdicionar = findViewById(R.id.fab_adicionar_imagem);

        setupRecyclerView();
        setupImagemLauncher();
        carregarImagens();

        fabAdicionar.setOnClickListener(v -> selecionarImagem());
    }

    private void setupImagemLauncher() {
        imagemLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imagemUri = result.getData().getData();
                        if (imagemUri != null) {
                            enviarImagemParaFirebase(imagemUri);
                        }
                    }
                }
        );
    }

    private void selecionarImagem() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagemLauncher.launch(intent);
    }

    private void enviarImagemParaFirebase(Uri imagemUri) {
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Fazendo upload da imagem...", Toast.LENGTH_SHORT).show();

        String nomeImagem = "veiculo_" + veiculoId + "_" + System.currentTimeMillis();
        firebaseManager.uploadImagemVeiculo(imagemUri, nomeImagem, new FirebaseManager.OnUploadCompleteListener() {
            @Override
            public void onUploadComplete(String downloadUrl) {
                salvarImagemNoBancoDados(downloadUrl);
            }

            @Override
            public void onUploadFailed(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Erro ao fazer upload", exception);
                Toast.makeText(GaleriaVeiculoActivity.this, "Erro ao fazer upload: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void salvarImagemNoBancoDados(String downloadUrl) {
        ImagemVeiculo novaImagem = new ImagemVeiculo();
        novaImagem.setVeiculoId(veiculoId);
        novaImagem.setUrlImagem(downloadUrl);
        novaImagem.setDataCadastro(System.currentTimeMillis());

        firebaseManager.adicionarImagemVeiculo(novaImagem)
                .addOnSuccessListener(ref -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(GaleriaVeiculoActivity.this, "Imagem adicionada com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarImagens();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Erro ao salvar imagem", e);
                    Toast.makeText(GaleriaVeiculoActivity.this, "Erro ao salvar imagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupRecyclerView() {
        adapter = new ImagemVeiculoAdapter(imagens, (imagem, position) ->
                Toast.makeText(this, "Imagem " + (position + 1), Toast.LENGTH_SHORT).show());
        recyclerImagens.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerImagens.setAdapter(adapter);
    }

    private void carregarImagens() {
        progressBar.setVisibility(View.VISIBLE);
        textVazio.setVisibility(View.GONE);

        firebaseManager.obterImagensVeiculo(veiculoId)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        imagens.clear();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            ImagemVeiculo imagem = doc.toObject(ImagemVeiculo.class);
                            if (imagem != null) {
                                imagem.setId(doc.getId());
                                imagens.add(imagem);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        textVazio.setVisibility(imagens.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Log.e(TAG, "Erro ao carregar imagens", task.getException());
                        Toast.makeText(this, "Erro ao carregar imagens", Toast.LENGTH_SHORT).show();
                        textVazio.setVisibility(View.VISIBLE);
                    }
                });
    }
}
