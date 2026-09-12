package net.kdt.pojavlaunch.modloaders;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;

import java.util.ArrayList;
import java.util.List;

public class ModItemAdapter extends RecyclerView.Adapter<ModItemAdapter.ModViewHolder> {

    public interface OnModInstallListener {
        void onInstall(ModDetail modDetail, int selectedVersion);
    }

    private final List<ModItem> mMods = new ArrayList<>();
    private final OnModInstallListener mInstallListener;

    public ModItemAdapter(OnModInstallListener listener) {
        mInstallListener = listener;
    }

    public void setMods(ModItem[] mods) {
        mMods.clear();
        if (mods != null) {
            for (ModItem mod : mods) {
                mMods.add(mod);
            }
        }
        notifyDataSetChanged();
    }

    public void addMods(ModItem[] mods) {
        int startPos = mMods.size();
        if (mods != null) {
            for (ModItem mod : mods) {
                mMods.add(mod);
            }
        }
        notifyItemRangeInserted(startPos, mods != null ? mods.length : 0);
    }

    @NonNull
    @Override
    public ModViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_mod, parent, false);
        return new ModViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ModViewHolder holder, int position) {
        ModItem mod = mMods.get(position);
        holder.bind(mod);
    }

    @Override
    public int getItemCount() {
        return mMods.size();
    }

    class ModViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name, summary;

        ModViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.mod_icon);
            name = itemView.findViewById(R.id.mod_name);
            summary = itemView.findViewById(R.id.mod_summary);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    ModItem mod = mMods.get(pos);
                    showVersionDialog(v.getContext(), mod);
                }
            });
        }

        void bind(ModItem mod) {
            name.setText(mod.name);
            summary.setText(mod.summary);
        }

        private void showVersionDialog(Context context, ModItem mod) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(mod.name)
                    .setMessage(R.string.loading_versions)
                    .show();

            new Thread(() -> {
                try {
                    CurseforgeApi api = new CurseforgeApi("YOUR_API_KEY");
                    ModDetail modDetail = api.getModDetails(mod);

                    ((android.app.Activity) context).runOnUiThread(() -> {
                        if (modDetail != null && modDetail.versionNames != null) {
                            builder.dismiss();
                            AlertDialog.Builder versionBuilder = new AlertDialog.Builder(context);
                            versionBuilder.setTitle(R.string.select_version)
                                    .setSingleChoiceItems(modDetail.versionNames, 0, null)
                                    .setPositiveButton(R.string.install_button, (dialog, which) -> {
                                        int selectedVersion = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                                        if (mInstallListener != null) {
                                            mInstallListener.onInstall(modDetail, selectedVersion);
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .show();
                        }
                    });
                } catch (Exception e) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        builder.dismiss();
                        AlertDialog.Builder errorBuilder = new AlertDialog.Builder(context);
                        errorBuilder.setTitle(R.string.error_title)
                                .setMessage(R.string.version_load_failed)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    });
                }
            }).start();
        }
    }
}
