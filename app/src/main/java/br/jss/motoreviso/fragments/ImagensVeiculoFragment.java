package br.jss.motoreviso.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import br.jss.motoreviso.R;
import br.jss.motoreviso.adapters.ImagemVeiculoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.ImagemVeiculo;

import java.util.ArrayList;
import java.util.List;

public class ImagensVeiculoFragment extends Fragment {
    private static final String TAG = "ImagensVeiculoFragment";

    private String veiculoId;
    private RecyclerView recyclerImagens;
    private ProgressBar progressBar;
    private TextView textVazio;
    private FirebaseManager firebaseManager;
    private List<ImagemVeiculo> imagens;
    private ImagemVeiculoAdapter adapter;

    public static ImagensVeiculoFragment newInstance(String veiculoId) {
        ImagensVeiculoFragment fragment = new ImagensVeiculoFragment();
        Bundle args = new Bundle();
        args.putString("VEICULO_ID", veiculoId);
        fragment.setArguments(args);
        return fragment;
    }

    public ImagensVeiculoFragment(String veiculoId) {
        this.veiculoId = veiculoId;
    }

    public ImagensVeiculoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_imagens_veiculo, container, false);

        recyclerImagens = view.findViewById(R.id.recycler_imagens_veiculo);
        progressBar = view.findViewById(R.id.progress_bar);
        textVazio = view.findViewById(R.id.text_vazio);

        if (veiculoId == null && getArguments() != null) {
            veiculoId = getArguments().getString("VEICULO_ID");
        }

        firebaseManager = FirebaseManager.getInstance();
        imagens = new ArrayList<>();

        setupRecyclerView();

        if (veiculoId != null) {
            carregarImagensVeiculo();
        } else {
            textVazio.setVisibility(View.VISIBLE);
        }

        return view;
    }

    private void setupRecyclerView() {
        adapter = new ImagemVeiculoAdapter(imagens);
        recyclerImagens.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerImagens.setAdapter(adapter);
    }

    private void carregarImagensVeiculo() {
        progressBar.setVisibility(View.VISIBLE);
        textVazio.setVisibility(View.GONE);

        firebaseManager.obterImagensVeiculo(veiculoId)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
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
                        textVazio.setVisibility(View.VISIBLE);
                    }
                });
    }
}
