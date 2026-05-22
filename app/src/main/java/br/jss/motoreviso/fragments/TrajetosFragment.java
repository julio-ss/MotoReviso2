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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.MapTrajetoActivity;
import br.jss.motoreviso.adapters.TrajetoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Trajeto;

public class TrajetosFragment extends Fragment {
    private static final String TAG = "TrajetosFragment";

    private RecyclerView recyclerView;
    private TrajetoAdapter adapter;
    private List<Trajeto> trajetos;
    private ProgressBar progressBar;
    private TextView textVazioMensagem;
    private FirebaseManager firebaseManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trajetos, container, false);

        recyclerView = view.findViewById(R.id.recycler_trajetos);
        progressBar = view.findViewById(R.id.progress_bar);
        textVazioMensagem = view.findViewById(R.id.text_vazio);

        firebaseManager = FirebaseManager.getInstance();
        trajetos = new ArrayList<>();
        setupRecyclerView();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        carregarTrajetos();
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

    private void carregarTrajetos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            textVazioMensagem.setText("Faça login para ver seus trajetos");
            textVazioMensagem.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        textVazioMensagem.setVisibility(View.GONE);

        firebaseManager.obterTrajetosUsuario(user.getUid())
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        trajetos.clear();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Trajeto t = doc.toObject(Trajeto.class);
                            if (t != null) {
                                t.setId(doc.getId());
                                trajetos.add(t);
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
                            textVazioMensagem.setText("Nenhum trajeto registrado.\nInicie um rastreamento para começar.");
                            textVazioMensagem.setVisibility(View.VISIBLE);
                        } else {
                            textVazioMensagem.setVisibility(View.GONE);
                        }
                    } else {
                        Log.e(TAG, "Erro ao carregar trajetos", task.getException());
                        textVazioMensagem.setText("Erro ao carregar trajetos.\nVerifique sua conexão.");
                        textVazioMensagem.setVisibility(View.VISIBLE);
                    }
                });
    }
}
