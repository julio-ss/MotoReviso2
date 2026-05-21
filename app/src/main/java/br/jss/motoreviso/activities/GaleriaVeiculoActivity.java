package br.jss.motoreviso.activities;

import android.os.Bundle;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.ImagemVeiculo;

import java.util.ArrayList;
import java.util.List;

public class GaleriaVeiculoActivity extends AppCompatActivity {
    private String veiculoId;
    private GridView gridView;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;
    private List<ImagemVeiculo> imagens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galeria_veiculo);

        veiculoId = getIntent().getStringExtra("VEICULO_ID");
        if (veiculoId == null) {
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();
        imagens = new ArrayList<>();

        gridView = findViewById(R.id.grid_imagens);
        progressBar = findViewById(R.id.progress_bar);

        carregarImagens();
    }

    private void carregarImagens() {
        progressBar.setVisibility(android.view.View.VISIBLE);

        firebaseManager.obterImagensVeiculo(veiculoId)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(android.view.View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        imagens.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                            ImagemVeiculo imagem = doc.toObject(ImagemVeiculo.class);
                            if (imagem != null) {
                                imagem.setId(doc.getId());
                                imagens.add(imagem);
                            }
                        }

                        if (imagens.isEmpty()) {
                            Toast.makeText(this, "Nenhuma imagem cadastrada", Toast.LENGTH_SHORT).show();
                        } else {
                            // Aqui você pode adicionar um adapter de galeria se desejar
                            Toast.makeText(this, imagens.size() + " imagens encontradas", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Erro ao carregar imagens", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}