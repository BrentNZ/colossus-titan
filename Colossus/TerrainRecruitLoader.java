/* Generated By:JavaCC: Do not edit this line. TerrainRecruitLoader.java */
import java.util.*;

public class TerrainRecruitLoader implements TerrainRecruitLoaderConstants {
    Hashtable carToRecruits = new Hashtable();
    Hashtable carToName = new Hashtable();
    Hashtable carToColor = new Hashtable();
    public char[] terrains = null;

    class recruitNumber
    {
        public String name;
        public int number;
        public recruitNumber(String n, int i)
        {
            name = n; number = i;
        }
        public String toString()
        {
            return("(" + number + "," + name +")");
        }
    }
    public Creature[] getStartingCreatures()
    {
        Creature[] bc = new Creature[3];
        ArrayList to = getPossibleRecruits('T');
        bc[0] = (Creature)to.get(0);
        bc[1] = (Creature)to.get(1);
        bc[2] = (Creature)to.get(2);
        return(bc);
    }
    public String getTerrainName(char tc)
    {
        return((String)carToName.get(new Character(tc)));
    }
    public java.awt.Color getTerrainColor(char tc)
    {
        return((java.awt.Color)carToColor.get(new Character(tc)));
    }
    public ArrayList getPossibleRecruits(char terrain)
    {
        ArrayList al = (ArrayList)carToRecruits.get(new Character(terrain));
        ArrayList re = new ArrayList();
        for (int i = 0; i < al.size() ; i++)
        {
            recruitNumber tr = (recruitNumber)al.get(i);
            if (tr.number > 0)
            {
                re.add(Creature.getCreatureByName(tr.name));
            }
        }
        return(re);
    }
    public int numberOfRecruiterNeeded(Creature recruiter, Creature recruit, char terrain)
    {
        if (terrain != 'T') /* handle Tower with care */
        {
            ArrayList al = (ArrayList)carToRecruits.get(new Character(terrain));
            ArrayList pc = getPossibleRecruits(terrain);

            int iReer = pc.indexOf(recruiter);
            int iRe = pc.indexOf(recruit);
            if ((iReer == -1) || (iRe == -1))
            {
                return 99; /* can't recruit/be recruited here */
            }

            if (iReer >= iRe)
            {
                return 1; /* only one to recruit same or below */
            }

            if ((iReer+1) < iRe)
            {
                return 99; /* can't recruit 2 above */
            }

            return (((recruitNumber)al.get(iRe)).number);
        }
        /* Tower, special */
        ArrayList to = getPossibleRecruits(terrain); /* this is 'T' */
        if (recruit.getName().equals(((Creature)to.get(0)).getName()) ||
                    recruit.getName().equals(((Creature)to.get(1)).getName()) ||
                    recruit.getName().equals(((Creature)to.get(2)).getName()))
        { /* first three can be recruited by anything */
            return 0;
        }
        else if (recruit.getName().equals(((Creature)to.get(4)).getName()))
        {
            if (recruiter.getName().equals("Titan") ||
                recruiter.getName().equals(((Creature)to.get(4)).getName()))
            { /* fifth recruited by Titan or itself */
                return 1;
            }
            else
            {
                return 99;
            }
        }
        else if (recruit.getName().equals(((Creature)to.get(3)).getName()))
        { /* fourth recruited by any 3 or itself */
            if (recruiter.getName().equals(((Creature)to.get(3)).getName()))
            {
                return 1;
            }
            else
            {
                return 3;
            }
        }
        else
        {
            return 99;
        }
    }

  final public char r_char() throws ParseException {
    jj_consume_token(TERCAR);
      {if (true) return (token.image.charAt(0));}
    throw new Error("Missing return statement in function");
  }

  final public String r_chaine() throws ParseException {
    jj_consume_token(CHAINE);
        {if (true) return(new String(token.image));}
    throw new Error("Missing return statement in function");
  }

  final public int r_number() throws ParseException {
    jj_consume_token(NUMBER);
        {if (true) return(Integer.parseInt(token.image));}
    throw new Error("Missing return statement in function");
  }

  final public ArrayList r_allRecruit() throws ParseException {
    ArrayList temp;
    int i;
    String n;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case NUMBER:
      i = r_number();
      n = r_chaine();
      temp = r_allRecruit();
      temp.add(0, new recruitNumber(n, i)); {if (true) return temp;}
      break;
    default:
      jj_la1[0] = jj_gen;
      {if (true) return new ArrayList();}
    }
    throw new Error("Missing return statement in function");
  }

  final public int oneTerrain() throws ParseException {
    String t;
    String col;
    char tc;
    ArrayList rl;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case TERCAR:
      tc = r_char();
      col = r_chaine();
      t = r_chaine();
      rl = r_allRecruit();
      jj_consume_token(EOL);
        carToRecruits.put(new Character(tc), rl);
        carToName.put(new Character (tc), t);
        carToColor.put(new Character (tc), HTMLColor.stringToColor(col));
        if (terrains == null)
        {
            terrains = new char[1];
            terrains[0] = tc;
        } else {
            char[] t2 = new char[terrains.length + 1];
            for (int i = 0; i < terrains.length ; i++)
                t2[i] = terrains[i];
            t2[terrains.length] = tc;
            terrains = t2;
        }
        Log.debug("Adding recruits for " + t + " " + rl);
        {if (true) return(1);}
      break;
    case EOL:
      jj_consume_token(EOL);
        {if (true) return(0);}
      break;
    case 0:
      jj_consume_token(0);
        {if (true) return(-1);}
      break;
    default:
      jj_la1[1] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  public TerrainRecruitLoaderTokenManager token_source;
  SimpleCharStream jj_input_stream;
  public Token token, jj_nt;
  private int jj_ntk;
  private int jj_gen;
  final private int[] jj_la1 = new int[2];
  final private int[] jj_la1_0 = {0x20,0x91,};

  public TerrainRecruitLoader(java.io.InputStream stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new TerrainRecruitLoaderTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 2; i++) jj_la1[i] = -1;
  }

  public void ReInit(java.io.InputStream stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 2; i++) jj_la1[i] = -1;
  }

  public TerrainRecruitLoader(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new TerrainRecruitLoaderTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 2; i++) jj_la1[i] = -1;
  }

  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 2; i++) jj_la1[i] = -1;
  }

  public TerrainRecruitLoader(TerrainRecruitLoaderTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 2; i++) jj_la1[i] = -1;
  }

  public void ReInit(TerrainRecruitLoaderTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 2; i++) jj_la1[i] = -1;
  }

  final private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  final private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.Vector jj_expentries = new java.util.Vector();
  private int[] jj_expentry;
  private int jj_kind = -1;

  final public ParseException generateParseException() {
    jj_expentries.removeAllElements();
    boolean[] la1tokens = new boolean[10];
    for (int i = 0; i < 10; i++) {
      la1tokens[i] = false;
    }
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 2; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 10; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.addElement(jj_expentry);
      }
    }
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = (int[])jj_expentries.elementAt(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  final public void enable_tracing() {
  }

  final public void disable_tracing() {
  }

}
