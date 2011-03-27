package itensil.util;

/**
 * Simple 2-tuple
 * @param <AA>
 * @param <BB>
 */
public class Pair<AA, BB>
{
    public AA first;
    public BB second;
    
    public Pair() {}
    public Pair(AA aa, BB bb){first=aa; second=bb;}
}
