package tj.zmt.barcode;

/**
 * Created by Admin on 04.08.2016.
 */
public class ReadXML {

     public String ReadXML(String teg, String str, String def) {
        String txt=str.toUpperCase();
        teg=teg.toUpperCase();
        int pbeg,pend;
        pbeg=txt.indexOf("<"+teg+">");
        if (pbeg>-1)
        {
            pend=txt.indexOf("</"+teg+">",pbeg);
            if (pend>-1)
            {
                if (pbeg+teg.length()+2-pend==0) {
                    return def;
                }
                else {
                    return str.substring(pbeg+teg.length()+2 ,pend);
                }
            }
            else return def;
        }
        else {
            return def;
        }
    }
}
