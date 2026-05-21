package br.jss.motoreviso.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import br.jss.motoreviso.R;
import br.jss.motoreviso.adapters.ImagemVeiculoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.ImagemVeiculo;

import java.util.ArrayList;
import java.util.List;

public class GaleriaVeiculoActivity extends AppCompatActivity {
    private static final String TAG = "GaleriaVeiculoActivity";

    private String veiculoId;
    private RecyclerView recyclerImagens;
    private ProgressBar progressBar;
    private TextView textVazio;
    private FirebaseManager firebaseManager;
    private List<ImagemVeiculo> imagens;
    private ImagemVeiculoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galeria_veiculo);

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

        setupRecyclerView();
        carregarImagens();
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
