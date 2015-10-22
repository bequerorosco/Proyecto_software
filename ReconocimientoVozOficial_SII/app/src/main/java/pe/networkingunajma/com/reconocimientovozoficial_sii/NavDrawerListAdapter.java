package pe.networkingunajma.com.reconocimientovozoficial_sii;

/**
 * Created by user on 21/10/2015.
 */


import java.util.ArrayList;

java.util.ArrayList importación;
        android.app.Activity importación;
        android.content.Context importación;
        android.view.LayoutInflater importación;
        android.view.View importación;
        android.view.ViewGroup importación;
        android.widget.BaseAdapter importación;
        android.widget.ImageView importación;
        android.widget.TextView importación;


public class NavDrawerListAdapter extiende BaseAdapter {

    contexto Contexto privado;
    ArrayList<NavDrawerItem> navDrawerItems privadas; // CORREJIDO, Gracias a ANONIMO

    pública NavDrawerListAdapter (contexto Contexto, ArrayList navDrawerItems) {
        this.context = contexto;
        this.navDrawerItems = navDrawerItems;
    }

    @ Override
    public int getCount () {
        volver navDrawerItems.size ();
    }

    @ Override
    public Object getItem (posición int) {
        volver navDrawerItems.get (posición);
    }

    @ Override
    GetItemID pública de largo (int posición) {
        posición de retorno;
    }

    @ Override
    pública Vista getView (posición int, Ver convertView, ViewGroup padre) {
        si (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService (Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate (R.layout.drawer_list_item, null);
        }

        ImgIcon ImageView = (ImageView) convertView.findViewById (R.id.icon);
        TextView txtTitle = (TextView) convertView.findViewById (R.id.title);
        TextView txtCount = (TextView) convertView.findViewById (R.id.counter);

        imgIcon.setImageResource (navDrawerItems.get (posición) .getIcon ());
        txtTitle.setText (navDrawerItems.get (posición) .getTitle ());

        // Recuento mostrar
        // Comprobar si se ajusta o no visible
        si (navDrawerItems.get (posición) .getCounterVisibility ()) {
            txtCount.setText (navDrawerItems.get (posición) .getCount ());
        } else {
            // Ocultar la vista counter
            txtCount.setVisibility (View.GONE);
        }

        volver convertView;
    }
}