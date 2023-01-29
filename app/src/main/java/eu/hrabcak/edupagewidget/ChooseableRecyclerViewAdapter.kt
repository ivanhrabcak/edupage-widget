package eu.hrabcak.edupagewidget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.hrabcak.edupagewidget.widget.WidgetTheme

class ChooseableRecyclerViewAdapter(
    private val dataset: Array<WidgetTheme>,
    var chosenThemeIndex: Int = 0,
    val onThemeChanged: (WidgetTheme) -> Unit
) : RecyclerView.Adapter<ChooseableRecyclerViewAdapter.ViewHolder>() {
    class ViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        val textView: TextView
        val checkmark: ImageView

        init {
            textView = view.findViewById(R.id.theme_name)
            checkmark = view.findViewById(R.id.checkmark)
        }
    }

    init {
        onThemeChanged(dataset[chosenThemeIndex])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = dataset[position].name


        holder.checkmark.isVisible = position == chosenThemeIndex
        holder.textView.setOnClickListener {
            chosenThemeIndex = position
            notifyDataSetChanged()
            onThemeChanged(dataset[position])
        }
    }

    override fun getItemCount(): Int = dataset.size
}