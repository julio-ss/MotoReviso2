package br.jss.motoreviso.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.models.Trajeto;

public class TrajetoAdapter extends RecyclerView.Adapter<TrajetoAdapter.TrajetoViewHolder> {
    private List<Trajeto> trajetos;
    private OnTrajetoClickListener clickListener;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));

    public interface OnTrajetoClickListener {
        void onTrajetoClick(Trajeto trajeto);
    }

    public TrajetoAdapter(List<Trajeto> trajetos, OnTrajetoClickListener listener) {
        this.trajetos = trajetos;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public TrajetoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Configuration.getInstance().setUserAgentValue(parent.getContext().getPackageName());
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trajeto, parent, false);
        return new TrajetoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrajetoViewHolder holder, int position) {
        Trajeto trajeto = trajetos.get(position);
        holder.bind(trajeto);
    }

    @Override
    public int getItemCount() {
        return trajetos.size();
    }

    @Override
    public void onViewRecycled(@NonNull TrajetoViewHolder holder) {
        holder.cleanup();
        super.onViewRecycled(holder);
    }

    public class TrajetoViewHolder extends RecyclerView.ViewHolder {
        private TextView textData;
        private TextView textKmRodados;
        private TextView textVelocidadeMax;
        private TextView textDuracao;
        private MapView mapPreview;

        public TrajetoViewHolder(@NonNull View itemView) {
            super(itemView);
            textData = itemView.findViewById(R.id.text_data);
            textKmRodados = itemView.findViewById(R.id.text_km_rodados);
            textVelocidadeMax = itemView.findViewById(R.id.text_velocidade_max);
            textDuracao = itemView.findViewById(R.id.text_duracao);
            mapPreview = itemView.findViewById(R.id.map_preview);

            mapPreview.setMultiTouchControls(true);
            mapPreview.setClickable(false);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_ID && clickListener != null) {
                    clickListener.onTrajetoClick(trajetos.get(pos));
                }
            });
        }

        public void bind(Trajeto trajeto) {
            if (trajeto.getDataInicio() != null) {
                textData.setText(sdf.format(trajeto.getDataInicio()));
            }

            textKmRodados.setText(String.format("%.2f km",
                    trajeto.getKmRodados() != null ? trajeto.getKmRodados() : 0));

            textVelocidadeMax.setText(String.format("Máx: %.1f km/h",
                    trajeto.getVelocidadeMaxima() != null ? trajeto.getVelocidadeMaxima() : 0));

            if (trajeto.getDuracao() != null) {
                long horas = trajeto.getDuracao() / 60;
                long minutos = trajeto.getDuracao() % 60;
                textDuracao.setText(String.format("%02d:%02d", horas, minutos));
            }

            carregarPreviewMapa(trajeto);
        }

        public void cleanup() {
            if (mapPreview != null) {
                try {
                    mapPreview.onDetach();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void carregarPreviewMapa(Trajeto trajeto) {
            try {
                mapPreview.getOverlays().clear();

                List<Trajeto.Ponto> pontos = trajeto.getPontos();
                if (pontos == null || pontos.isEmpty()) {
                    return;
                }

                final List<GeoPoint> geoPoints = new ArrayList<>();
                double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
                double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

                for (Trajeto.Ponto ponto : pontos) {
                    if (ponto != null && ponto.getLatitude() != null && ponto.getLongitude() != null) {
                        GeoPoint geoPoint = new GeoPoint(ponto.getLatitude(), ponto.getLongitude());
                        geoPoints.add(geoPoint);

                        minLat = Math.min(minLat, ponto.getLatitude());
                        maxLat = Math.max(maxLat, ponto.getLatitude());
                        minLon = Math.min(minLon, ponto.getLongitude());
                        maxLon = Math.max(maxLon, ponto.getLongitude());
                    }
                }

                if (geoPoints.isEmpty()) {
                    return;
                }

                String origem = trajeto.getOrigem() != null ? trajeto.getOrigem() : "Início";
                String destino = trajeto.getDestino() != null ? trajeto.getDestino() : "Fim";

                Polyline polyline = new Polyline(mapPreview);
                polyline.setPoints(geoPoints);
                polyline.setWidth(10f);
                polyline.setColor(Color.parseColor("#00D4FF"));
                polyline.setGeodesic(true);
                mapPreview.getOverlays().add(0, polyline);

                Marker markerInicio = new Marker(mapPreview);
                markerInicio.setPosition(geoPoints.get(0));
                markerInicio.setTitle("De: " + origem);
                markerInicio.setSnippet("Ponto de partida");
                markerInicio.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                mapPreview.getOverlays().add(markerInicio);

                if (geoPoints.size() > 1) {
                    Marker markerFim = new Marker(mapPreview);
                    markerFim.setPosition(geoPoints.get(geoPoints.size() - 1));
                    markerFim.setTitle("Para: " + destino);
                    markerFim.setSnippet("Ponto de chegada");
                    markerFim.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    mapPreview.getOverlays().add(markerFim);
                }

                final double finalMinLat = minLat;
                final double finalMaxLat = maxLat;
                final double finalMinLon = minLon;
                final double finalMaxLon = maxLon;

                mapPreview.post(() -> {
                    try {
                        if (geoPoints.size() == 1) {
                            mapPreview.getController().setZoom(16);
                            mapPreview.getController().setCenter(geoPoints.get(0));
                        } else {
                            double latSpan = finalMaxLat - finalMinLat;
                            double lonSpan = finalMaxLon - finalMinLon;
                            double maxSpan = Math.max(latSpan, lonSpan);

                            int zoom = 15;
                            if (maxSpan < 0.005) zoom = 18;
                            else if (maxSpan < 0.01) zoom = 17;
                            else if (maxSpan < 0.02) zoom = 16;
                            else if (maxSpan < 0.05) zoom = 15;
                            else if (maxSpan < 0.1) zoom = 14;
                            else if (maxSpan < 0.5) zoom = 12;
                            else if (maxSpan < 1.0) zoom = 11;
                            else if (maxSpan < 5.0) zoom = 9;
                            else zoom = 7;

                            double centerLat = (finalMinLat + finalMaxLat) / 2;
                            double centerLon = (finalMinLon + finalMaxLon) / 2;
                            GeoPoint center = new GeoPoint(centerLat, centerLon);

                            mapPreview.getController().setZoom(zoom);
                            mapPreview.getController().setCenter(center);
                        }
                        mapPreview.invalidate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}