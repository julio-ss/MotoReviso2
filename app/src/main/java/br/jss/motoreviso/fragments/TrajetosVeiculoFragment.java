package br.jss.motoreviso.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QuerySnapshot;

import android.content.Intent;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.MapTrajetoActivity;
import br.jss.motoreviso.adapters.TrajetoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Trajeto;

import java.util.ArrayList;
import java.util.List;

public class TrajetosVeiculoFragment extends Fragment {
    private String veiculoId;
    private RecyclerView recyclerView;
    private TrajetoAdapter adapter;
    private List<Trajeto> trajetos;
    private ProgressBar progressBar;
    private TextView textVazio;
    private FirebaseManager firebaseManager;

    public TrajetosVeiculoFragment(String veiculoId) {
        this.veiculoId = veiculoId;
    }

    public TrajetosVeiculoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trajetos_veiculo, container, false);

        recyclerView = view.findViewById(R.id.recycler_trajetos_veiculo);
        progressBar = view.findViewById(R.id.progress_bar);
        textVazio = view.findViewById(R.id.text_vazio);

        if (veiculoId == null) {
            veiculoId = getArguments() != null ? getArguments().getString("VEICULO_ID") : null;
        }

        firebaseManager = FirebaseManager.getInstance();
        trajetos = new ArrayList<>();

        setupRecyclerView();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (veiculoId != null) {
            carregarTrajetosVeiculo();
        }
    }

    private void setupRecyclerView() {
        adapter = new TrajetoAdapter(trajetos, trajeto -> {
            if (trajeto.getId() != null && getActivity() != null) {
                Intent intent = new Intent(getActivity(), MapTrajetoActivity.class);
                intent.putExtra("TRAJETO_ID", trajeto.getId());
                startActivity(intent);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void carregarTrajetosVeiculo() {
        progressBar.setVisibility(View.VISIBLE);
        textVazio.setVisibility(View.GONE);

        firebaseManager.obterTrajetosVeiculo(veiculoId)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        trajetos.clear();
                        QuerySnapshot snapshot = task.getResult();

                        if (snapshot != null) {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                                Trajeto trajeto = doc.toObject(Trajeto.class);
                                if (trajeto != null) {
                                    trajeto.setId(doc.getId());
                                    trajetos.add(trajeto);
                                }
                            }
                        }

                        adapter.notifyDataSetChanged();
                        textVazio.setVisibility(trajetos.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        android.util.Log.e("TrajetosVeiculoFrag",
                                "Erro ao carregar trajetos", task.getException());
                        textVazio.setText("Erro ao carregar trajetos.\nVerifique sua conexão.");
                        textVazio.setVisibility(View.VISIBLE);
                    }
                });
    }
}