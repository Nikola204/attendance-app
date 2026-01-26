package com.example.studentqrscanner.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.util.CustomQrGenerator;

/**
 * Adapter za prikaz i izbor QR stilova
 */
public class QrStyleAdapter extends RecyclerView.Adapter<QrStyleAdapter.StyleViewHolder> {

    public interface OnStyleSelectedListener {
        void onStyleSelected(CustomQrGenerator.QrStyle style);
    }

    private final Context context;
    private final CustomQrGenerator.QrStyle[] styles;
    private final boolean isStudent;
    private CustomQrGenerator.QrStyle selectedStyle;
    private final OnStyleSelectedListener listener;

    public QrStyleAdapter(Context context, CustomQrGenerator.QrStyle[] styles,
                          boolean isStudent, CustomQrGenerator.QrStyle selectedStyle,
                          OnStyleSelectedListener listener) {
        this.context = context;
        this.styles = styles;
        this.isStudent = isStudent;
        this.selectedStyle = selectedStyle;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StyleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_qr_style, parent, false);
        return new StyleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StyleViewHolder holder, int position) {
        CustomQrGenerator.QrStyle style = styles[position];

        // Postavi naziv
        holder.tvStyleName.setText(style.getDisplayName());

        // Generiši preview QR kod
        CustomQrGenerator generator = new CustomQrGenerator(context);
        String previewContent = isStudent ? "STUDENT_PREVIEW" : "LECTURE_PREVIEW";
        Bitmap qrBitmap = generator.generateStyledQr(previewContent, style, "PREVIEW");

        if (qrBitmap != null) {
            holder.ivQrPreview.setImageBitmap(qrBitmap);
        }

        // Prikaži checkmark ako je ovo odabrani stil
        boolean isSelected = style == selectedStyle;
        holder.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        // Highlightuj odabrani stil sa većom elevacijom
        holder.cardQrStyle.setCardElevation(isSelected ? 12f : 4f);

        // Klik listener
        holder.itemView.setOnClickListener(v -> {
            selectedStyle = style;
            listener.onStyleSelected(style);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return styles.length;
    }

    public void setSelectedStyle(CustomQrGenerator.QrStyle style) {
        this.selectedStyle = style;
        notifyDataSetChanged();
    }

    static class StyleViewHolder extends RecyclerView.ViewHolder {
        CardView cardQrStyle;
        ImageView ivQrPreview;
        ImageView ivSelected;
        TextView tvStyleName;

        public StyleViewHolder(@NonNull View itemView) {
            super(itemView);
            cardQrStyle = (CardView) itemView;
            ivQrPreview = itemView.findViewById(R.id.ivQrPreview);
            ivSelected = itemView.findViewById(R.id.ivSelected);
            tvStyleName = itemView.findViewById(R.id.tvStyleName);
        }
    }
}
