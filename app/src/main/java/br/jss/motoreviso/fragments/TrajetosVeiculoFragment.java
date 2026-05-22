package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.MapTrajetoActivity;
import br.jss.motoreviso.adapters.TrajetoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Trajeto;

public class TrajetosVeiculoFragment extends Fragment {
    private static final String TAG = "TrajetosVeiculoFrag";

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

    public TrajetosVeiculoFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trajetos_veiculo, container, false);

        recyclerView = view.findViewById(R.id.recycler_trajetos_veiculo);
        progressBar = view.findViewById(R.id.progress_bar);
        textVazio = view.findViewById(R.id.text_vazio);

        if (veiculoId == null && getArguments() != null) {
            veiculoId = getArguments().getString("VEICULO_ID");
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
        } else {
            textVazio.setText("Veículo não identificado");
            textVazio.setVisibility(View.VISIBLE);
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

        // Sem orderBy — evita índice composto no Firestore. Ordena no cliente.
        firebaseManager.obterTrajetosVeiculo(veiculoId)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        trajetos.clear();
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                Trajeto trajeto = doc.toObject(Trajeto.class);
                                if (trajeto != null) {
                                    trajeto.setId(doc.getId());
                                    trajetos.add(trajeto);
                                }
                            }
                        }

                        // Ordena por dataInicio decrescente no cliente
                        Collections.sort(trajetos, (a, b) -> {
                            Long dA = a.getDataInicio() != null ? a.getDataInicio() : 0L;
                            Long dB = b.getDataInicio() != null ? b.getDataInicio() : 0L;
                            return dB.compareTo(dA);
                        });

                        adapter.notifyDataSetChanged();

                        if (trajetos.isEmpty()) {
                            textVazio.setText("Nenhum trajeto registrado para este veículo.");
                            textVazio.setVisibility(View.VISIBLE);
                        } else {
                            textVazio.setVisibility(View.GONE);
                        }
                    } else {
                        Log.e(TAG, "Erro ao carregar trajetos", task.getException());
                        textVazio.setText("Erro ao carregar trajetos.\nVerifique sua conexão.");
                        textVazio.setVisibility(View.VISIBLE);
                    }
                });
    }
}
