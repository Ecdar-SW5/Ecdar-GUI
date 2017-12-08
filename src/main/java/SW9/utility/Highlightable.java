package SW9.utility;

/***
 * Interface implemented in classes that can be highlighted (e.g. change color on mouseover)
 * Reminds of the Selectable interface, because selected items are also highlighted in some way
 */
public interface Highlightable {
    public void highlight();
    public void unhighlight();
}
