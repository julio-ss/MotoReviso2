package br.jss.motoreviso.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.QuerySnapshot;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.ImagemVeiculo;

import java.util.ArrayList;
import java.util.List;

public class ImagensVeiculoFragment extends Fragment {
    private String veiculoId;
    private GridView gridView;
    private ProgressBar progressBar;
    private TextView textVazio;
    private FirebaseManager firebaseManager;
    private List<ImagemVeiculo> imagens;

    public ImagensVeiculoFragment(String veiculoId) {
        this.veiculoId = veiculoId;
    }

    public ImagensVeiculoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_imagens_veiculo, container, false);

        gridView = view.findViewById(R.id.grid_imagens_veiculo);
        progressBar = view.findViewById(R.id.progress_bar);
        textVazio = view.findViewById(R.id.text_vazio);

        if (veiculoId == null) {
            veiculoId = getArguments() != null ? getArguments().getString("VEICULO_ID") : null;
        }

        firebaseManager = FirebaseManager.getInstance();
        imagens = new ArrayList<>();

        if (veiculoId != null) {
            carregarImagensVeiculo();
        }

        return view;
    }

    private void carregarImagensVeiculo() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.obterImagensVeiculo(veiculoId)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        imagens.clear();
                        QuerySnapshot snapshot = task.getResult();

                        if (snapshot != null) {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                                ImagemVeiculo imagem = doc.toObject(ImagemVeiculo.class);
                                if (imagem != null) {
                                    imagem.setId(doc.getId());
                                    imagens.add(imagem);
                                }
                            }
                        }

                        if (imagens.isEmpty()) {
                            textVazio.setVisibility(View.VISIBLE);
                        } else {
                            textVazio.setVisibility(View.GONE);
                        }
                    }
                });
    }
}