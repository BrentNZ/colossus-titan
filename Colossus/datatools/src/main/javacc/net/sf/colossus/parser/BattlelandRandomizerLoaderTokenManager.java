/* Generated By:JavaCC: Do not edit this line. BattlelandRandomizerLoaderTokenManager.java */
package net.sf.colossus.parser;
import java.util.*;
import java.util.logging.*;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.HazardTerrain;

/** Token Manager. */
public class BattlelandRandomizerLoaderTokenManager implements BattlelandRandomizerLoaderConstants
{

  /** Debug output. */
  public  java.io.PrintStream debugStream = System.out;
  /** Set debug output. */
  public  void setDebugStream(java.io.PrintStream ds) { debugStream = ds; }
private final int jjStopStringLiteralDfa_0(int pos, long active0)
{
   switch (pos)
   {
      case 0:
         if ((active0 & 0x2000000L) != 0L)
            return 4;
         if ((active0 & 0x1fff00L) != 0L)
         {
            jjmatchedKind = 32;
            return 4;
         }
         if ((active0 & 0x80L) != 0L)
         {
            jjmatchedKind = 32;
            return 1;
         }
         return -1;
      case 1:
         if ((active0 & 0x1fff80L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 1;
            return 4;
         }
         return -1;
      case 2:
         if ((active0 & 0x1fff80L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 2;
            return 4;
         }
         return -1;
      case 3:
         if ((active0 & 0x2080L) != 0L)
            return 4;
         if ((active0 & 0x1fdf00L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 3;
            return 4;
         }
         return -1;
      case 4:
         if ((active0 & 0xc0200L) != 0L)
            return 4;
         if ((active0 & 0x13dd00L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 4;
            return 4;
         }
         return -1;
      case 5:
         if ((active0 & 0x12d900L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 5;
            return 4;
         }
         if ((active0 & 0x10400L) != 0L)
            return 4;
         return -1;
      case 6:
         if ((active0 & 0x12d800L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 6;
            return 4;
         }
         if ((active0 & 0x100L) != 0L)
            return 4;
         return -1;
      case 7:
         if ((active0 & 0x10c000L) != 0L)
            return 4;
         if ((active0 & 0x21800L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 7;
            return 4;
         }
         return -1;
      case 8:
         if ((active0 & 0x1000L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 8;
            return 4;
         }
         if ((active0 & 0x20800L) != 0L)
            return 4;
         return -1;
      case 9:
         if ((active0 & 0x1000L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 9;
            return 4;
         }
         return -1;
      case 10:
         if ((active0 & 0x1000L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 10;
            return 4;
         }
         return -1;
      case 11:
         if ((active0 & 0x1000L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 11;
            return 4;
         }
         return -1;
      case 12:
         if ((active0 & 0x1000L) != 0L)
         {
            jjmatchedKind = 33;
            jjmatchedPos = 12;
            return 4;
         }
         return -1;
      default :
         return -1;
   }
}
private final int jjStartNfa_0(int pos, long active0)
{
   return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0), pos + 1);
}
private int jjStopAtPos(int pos, int kind)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   return pos + 1;
}
private int jjMoveStringLiteralDfa0_0()
{
   switch(curChar)
   {
      case 10:
         return jjStopAtPos(0, 6);
      case 40:
         return jjStopAtPos(0, 26);
      case 41:
         return jjStopAtPos(0, 27);
      case 42:
         return jjStopAtPos(0, 30);
      case 44:
         return jjStopAtPos(0, 24);
      case 45:
         return jjStopAtPos(0, 28);
      case 46:
         return jjStartNfaWithStates_0(0, 25, 4);
      case 61:
         return jjStopAtPos(0, 29);
      case 65:
         return jjMoveStringLiteralDfa1_0(0x80L);
      case 72:
         return jjMoveStringLiteralDfa1_0(0x4100L);
      case 76:
         return jjMoveStringLiteralDfa1_0(0x200L);
      case 80:
         return jjMoveStringLiteralDfa1_0(0x2000L);
      case 83:
         return jjMoveStringLiteralDfa1_0(0x121c00L);
      case 84:
         return jjMoveStringLiteralDfa1_0(0xc0000L);
      case 108:
         return jjMoveStringLiteralDfa1_0(0x8000L);
      case 117:
         return jjMoveStringLiteralDfa1_0(0x10000L);
      default :
         return jjMoveNfa_0(0, 0);
   }
}
private int jjMoveStringLiteralDfa1_0(long active0)
{
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(0, active0);
      return 1;
   }
   switch(curChar)
   {
      case 65:
         return jjMoveStringLiteralDfa2_0(active0, 0x2300L);
      case 69:
         return jjMoveStringLiteralDfa2_0(active0, 0x4000L);
      case 73:
         return jjMoveStringLiteralDfa2_0(active0, 0x80000L);
      case 79:
         return jjMoveStringLiteralDfa2_0(active0, 0x40400L);
      case 82:
         return jjMoveStringLiteralDfa2_0(active0, 0x80L);
      case 84:
         return jjMoveStringLiteralDfa2_0(active0, 0x20000L);
      case 85:
         return jjMoveStringLiteralDfa2_0(active0, 0x101800L);
      case 101:
         return jjMoveStringLiteralDfa2_0(active0, 0x8000L);
      case 115:
         return jjMoveStringLiteralDfa2_0(active0, 0x10000L);
      default :
         break;
   }
   return jjStartNfa_0(0, active0);
}
private int jjMoveStringLiteralDfa2_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(0, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(1, active0);
      return 2;
   }
   switch(curChar)
   {
      case 65:
         return jjMoveStringLiteralDfa3_0(active0, 0x20000L);
      case 66:
         return jjMoveStringLiteralDfa3_0(active0, 0x100a00L);
      case 69:
         return jjMoveStringLiteralDfa3_0(active0, 0x80L);
      case 73:
         return jjMoveStringLiteralDfa3_0(active0, 0x2000L);
      case 77:
         return jjMoveStringLiteralDfa3_0(active0, 0x400L);
      case 82:
         return jjMoveStringLiteralDfa3_0(active0, 0x1000L);
      case 84:
         return jjMoveStringLiteralDfa3_0(active0, 0x80000L);
      case 87:
         return jjMoveStringLiteralDfa3_0(active0, 0x40000L);
      case 88:
         return jjMoveStringLiteralDfa3_0(active0, 0x4000L);
      case 90:
         return jjMoveStringLiteralDfa3_0(active0, 0x100L);
      case 101:
         return jjMoveStringLiteralDfa3_0(active0, 0x10000L);
      case 102:
         return jjMoveStringLiteralDfa3_0(active0, 0x8000L);
      default :
         break;
   }
   return jjStartNfa_0(1, active0);
}
private int jjMoveStringLiteralDfa3_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(1, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(2, active0);
      return 3;
   }
   switch(curChar)
   {
      case 65:
         if ((active0 & 0x80L) != 0L)
            return jjStartNfaWithStates_0(3, 7, 4);
         return jjMoveStringLiteralDfa4_0(active0, 0x100L);
      case 69:
         return jjMoveStringLiteralDfa4_0(active0, 0x40600L);
      case 76:
         return jjMoveStringLiteralDfa4_0(active0, 0x80000L);
      case 82:
         if ((active0 & 0x2000L) != 0L)
            return jjStartNfaWithStates_0(3, 13, 4);
         return jjMoveStringLiteralDfa4_0(active0, 0x21000L);
      case 83:
         return jjMoveStringLiteralDfa4_0(active0, 0x4800L);
      case 84:
         return jjMoveStringLiteralDfa4_0(active0, 0x100000L);
      case 100:
         return jjMoveStringLiteralDfa4_0(active0, 0x10000L);
      case 116:
         return jjMoveStringLiteralDfa4_0(active0, 0x8000L);
      default :
         break;
   }
   return jjStartNfa_0(2, active0);
}
private int jjMoveStringLiteralDfa4_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(2, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(3, active0);
      return 4;
   }
   switch(curChar)
   {
      case 69:
         if ((active0 & 0x80000L) != 0L)
            return jjStartNfaWithStates_0(4, 19, 4);
         break;
      case 73:
         return jjMoveStringLiteralDfa5_0(active0, 0x104000L);
      case 76:
         if ((active0 & 0x200L) != 0L)
            return jjStartNfaWithStates_0(4, 9, 4);
         break;
      case 79:
         return jjMoveStringLiteralDfa5_0(active0, 0x1400L);
      case 82:
         if ((active0 & 0x40000L) != 0L)
            return jjStartNfaWithStates_0(4, 18, 4);
         return jjMoveStringLiteralDfa5_0(active0, 0x100L);
      case 84:
         return jjMoveStringLiteralDfa5_0(active0, 0x20800L);
      case 111:
         return jjMoveStringLiteralDfa5_0(active0, 0x8000L);
      case 117:
         return jjMoveStringLiteralDfa5_0(active0, 0x10000L);
      default :
         break;
   }
   return jjStartNfa_0(3, active0);
}
private int jjMoveStringLiteralDfa5_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(3, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(4, active0);
      return 5;
   }
   switch(curChar)
   {
      case 68:
         return jjMoveStringLiteralDfa6_0(active0, 0x4100L);
      case 70:
         if ((active0 & 0x400L) != 0L)
            return jjStartNfaWithStates_0(5, 10, 4);
         break;
      case 76:
         return jjMoveStringLiteralDfa6_0(active0, 0x20000L);
      case 82:
         return jjMoveStringLiteralDfa6_0(active0, 0x800L);
      case 84:
         return jjMoveStringLiteralDfa6_0(active0, 0x100000L);
      case 85:
         return jjMoveStringLiteralDfa6_0(active0, 0x1000L);
      case 112:
         if ((active0 & 0x10000L) != 0L)
            return jjStartNfaWithStates_0(5, 16, 4);
         break;
      case 118:
         return jjMoveStringLiteralDfa6_0(active0, 0x8000L);
      default :
         break;
   }
   return jjStartNfa_0(4, active0);
}
private int jjMoveStringLiteralDfa6_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(4, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(5, active0);
      return 6;
   }
   switch(curChar)
   {
      case 65:
         return jjMoveStringLiteralDfa7_0(active0, 0x800L);
      case 69:
         return jjMoveStringLiteralDfa7_0(active0, 0x4000L);
      case 73:
         return jjMoveStringLiteralDfa7_0(active0, 0x20000L);
      case 76:
         return jjMoveStringLiteralDfa7_0(active0, 0x100000L);
      case 78:
         return jjMoveStringLiteralDfa7_0(active0, 0x1000L);
      case 83:
         if ((active0 & 0x100L) != 0L)
            return jjStartNfaWithStates_0(6, 8, 4);
         break;
      case 101:
         return jjMoveStringLiteralDfa7_0(active0, 0x8000L);
      default :
         break;
   }
   return jjStartNfa_0(5, active0);
}
private int jjMoveStringLiteralDfa7_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(5, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(6, active0);
      return 7;
   }
   switch(curChar)
   {
      case 67:
         return jjMoveStringLiteralDfa8_0(active0, 0x800L);
      case 68:
         return jjMoveStringLiteralDfa8_0(active0, 0x1000L);
      case 69:
         if ((active0 & 0x100000L) != 0L)
            return jjStartNfaWithStates_0(7, 20, 4);
         break;
      case 83:
         if ((active0 & 0x4000L) != 0L)
            return jjStartNfaWithStates_0(7, 14, 4);
         return jjMoveStringLiteralDfa8_0(active0, 0x20000L);
      case 114:
         if ((active0 & 0x8000L) != 0L)
            return jjStartNfaWithStates_0(7, 15, 4);
         break;
      default :
         break;
   }
   return jjStartNfa_0(6, active0);
}
private int jjMoveStringLiteralDfa8_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(6, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(7, active0);
      return 8;
   }
   switch(curChar)
   {
      case 73:
         return jjMoveStringLiteralDfa9_0(active0, 0x1000L);
      case 84:
         if ((active0 & 0x800L) != 0L)
            return jjStartNfaWithStates_0(8, 11, 4);
         else if ((active0 & 0x20000L) != 0L)
            return jjStartNfaWithStates_0(8, 17, 4);
         break;
      default :
         break;
   }
   return jjStartNfa_0(7, active0);
}
private int jjMoveStringLiteralDfa9_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(7, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(8, active0);
      return 9;
   }
   switch(curChar)
   {
      case 78:
         return jjMoveStringLiteralDfa10_0(active0, 0x1000L);
      default :
         break;
   }
   return jjStartNfa_0(8, active0);
}
private int jjMoveStringLiteralDfa10_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(8, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(9, active0);
      return 10;
   }
   switch(curChar)
   {
      case 71:
         return jjMoveStringLiteralDfa11_0(active0, 0x1000L);
      default :
         break;
   }
   return jjStartNfa_0(9, active0);
}
private int jjMoveStringLiteralDfa11_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(9, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(10, active0);
      return 11;
   }
   switch(curChar)
   {
      case 83:
         return jjMoveStringLiteralDfa12_0(active0, 0x1000L);
      default :
         break;
   }
   return jjStartNfa_0(10, active0);
}
private int jjMoveStringLiteralDfa12_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(10, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(11, active0);
      return 12;
   }
   switch(curChar)
   {
      case 79:
         return jjMoveStringLiteralDfa13_0(active0, 0x1000L);
      default :
         break;
   }
   return jjStartNfa_0(11, active0);
}
private int jjMoveStringLiteralDfa13_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(11, old0);
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(12, active0);
      return 13;
   }
   switch(curChar)
   {
      case 70:
         if ((active0 & 0x1000L) != 0L)
            return jjStartNfaWithStates_0(13, 12, 4);
         break;
      default :
         break;
   }
   return jjStartNfa_0(12, active0);
}
private int jjStartNfaWithStates_0(int pos, int kind, int state)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) { return pos + 1; }
   return jjMoveNfa_0(state, pos + 1);
}
static final long[] jjbitVec0 = {
   0x0L, 0x0L, 0xffffffffffffffffL, 0xffffffffffffffffL
};
private int jjMoveNfa_0(int startState, int curPos)
{
   int startsAt = 0;
   jjnewStateCnt = 18;
   int i = 1;
   jjstateSet[0] = startState;
   int kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  if ((0x3ff000000000000L & l) != 0L)
                  {
                     if (kind > 21)
                        kind = 21;
                     jjCheckNAddStates(0, 2);
                  }
                  else if (curChar == 35)
                     jjCheckNAddStates(3, 6);
                  else if (curChar == 34)
                     jjCheckNAdd(6);
                  else if (curChar == 46)
                  {
                     if (kind > 33)
                        kind = 33;
                     jjCheckNAdd(4);
                  }
                  break;
               case 1:
                  if ((0x3ff400000000000L & l) != 0L)
                  {
                     if (kind > 33)
                        kind = 33;
                     jjCheckNAdd(4);
                  }
                  if ((0x7e000000000000L & l) != 0L)
                  {
                     if (kind > 31)
                        kind = 31;
                  }
                  break;
               case 3:
                  if (curChar != 46)
                     break;
                  if (kind > 33)
                     kind = 33;
                  jjCheckNAdd(4);
                  break;
               case 4:
                  if ((0x3ff400000000000L & l) == 0L)
                     break;
                  if (kind > 33)
                     kind = 33;
                  jjCheckNAdd(4);
                  break;
               case 5:
                  if (curChar == 34)
                     jjCheckNAdd(6);
                  break;
               case 6:
                  if ((0xafffd80300000000L & l) != 0L)
                     jjCheckNAddTwoStates(6, 7);
                  break;
               case 7:
                  if (curChar == 34 && kind > 34)
                     kind = 34;
                  break;
               case 8:
                  if (curChar == 35)
                     jjCheckNAddStates(3, 6);
                  break;
               case 9:
                  if ((0xffffffffffffdbffL & l) != 0L)
                     jjCheckNAddTwoStates(9, 10);
                  break;
               case 10:
                  if (curChar == 13)
                     kind = 4;
                  break;
               case 11:
                  if ((0xffffffffffffdbffL & l) != 0L)
                     jjCheckNAddTwoStates(11, 12);
                  break;
               case 12:
                  if (curChar == 10)
                     kind = 4;
                  break;
               case 13:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 21)
                     kind = 21;
                  jjCheckNAddStates(0, 2);
                  break;
               case 14:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 21)
                     kind = 21;
                  jjCheckNAdd(14);
                  break;
               case 15:
                  if ((0x3ff000000000000L & l) != 0L)
                     jjCheckNAddTwoStates(15, 16);
                  break;
               case 16:
                  if (curChar != 46)
                     break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAdd(17);
                  break;
               case 17:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAdd(17);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  if ((0x7fffffe87fffffeL & l) != 0L)
                  {
                     if (kind > 33)
                        kind = 33;
                     jjCheckNAdd(4);
                  }
                  if ((0x7fffffe07fffffeL & l) != 0L)
                  {
                     if (kind > 32)
                        kind = 32;
                  }
                  if ((0x7e0000007eL & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 1;
                  break;
               case 1:
               case 4:
                  if ((0x7fffffe87fffffeL & l) == 0L)
                     break;
                  if (kind > 33)
                     kind = 33;
                  jjCheckNAdd(4);
                  break;
               case 2:
                  if ((0x7fffffe07fffffeL & l) != 0L && kind > 32)
                     kind = 32;
                  break;
               case 3:
                  if ((0x7fffffe87fffffeL & l) == 0L)
                     break;
                  if (kind > 33)
                     kind = 33;
                  jjCheckNAdd(4);
                  break;
               case 6:
                  if ((0x7fffffe87fffffeL & l) != 0L)
                     jjAddStates(7, 8);
                  break;
               case 9:
                  jjAddStates(9, 10);
                  break;
               case 11:
                  jjAddStates(11, 12);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               case 9:
                  if ((jjbitVec0[i2] & l2) != 0L)
                     jjAddStates(9, 10);
                  break;
               case 11:
                  if ((jjbitVec0[i2] & l2) != 0L)
                     jjAddStates(11, 12);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 18 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
static final int[] jjnextStates = {
   14, 15, 16, 9, 10, 11, 12, 6, 7, 9, 10, 11, 12, 
};

/** Token literal values. */
public static final String[] jjstrLiteralImages = {
"", null, null, null, null, null, "\12", "\101\122\105\101", 
"\110\101\132\101\122\104\123", "\114\101\102\105\114", "\123\117\115\105\117\106", 
"\123\125\102\123\124\122\101\103\124", "\123\125\122\122\117\125\116\104\111\116\107\123\117\106", 
"\120\101\111\122", "\110\105\130\123\111\104\105\123", "\154\145\146\164\157\166\145\162", 
"\165\163\145\144\165\160", "\123\124\101\122\124\114\111\123\124", "\124\117\127\105\122", 
"\124\111\124\114\105", "\123\125\102\124\111\124\114\105", null, null, null, "\54", "\56", "\50", 
"\51", "\55", "\75", "\52", null, null, null, null, null, null, null, };

/** Lexer state names. */
public static final String[] lexStateNames = {
   "DEFAULT",
};
static final long[] jjtoToken = {
   0x7ff7fffd1L, 
};
static final long[] jjtoSkip = {
   0xeL, 
};
protected SimpleCharStream input_stream;
private final int[] jjrounds = new int[18];
private final int[] jjstateSet = new int[36];
protected char curChar;
/** Constructor. */
public BattlelandRandomizerLoaderTokenManager(SimpleCharStream stream){
   if (SimpleCharStream.staticFlag)
      throw new Error("ERROR: Cannot use a static CharStream class with a non-static lexical analyzer.");
   input_stream = stream;
}

/** Constructor. */
public BattlelandRandomizerLoaderTokenManager(SimpleCharStream stream, int lexState){
   this(stream);
   SwitchTo(lexState);
}

/** Reinitialise parser. */
public void ReInit(SimpleCharStream stream)
{
   jjmatchedPos = jjnewStateCnt = 0;
   curLexState = defaultLexState;
   input_stream = stream;
   ReInitRounds();
}
private void ReInitRounds()
{
   int i;
   jjround = 0x80000001;
   for (i = 18; i-- > 0;)
      jjrounds[i] = 0x80000000;
}

/** Reinitialise parser. */
public void ReInit(SimpleCharStream stream, int lexState)
{
   ReInit(stream);
   SwitchTo(lexState);
}

/** Switch to specified lex state. */
public void SwitchTo(int lexState)
{
   if (lexState >= 1 || lexState < 0)
      throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", TokenMgrError.INVALID_LEXICAL_STATE);
   else
      curLexState = lexState;
}

protected Token jjFillToken()
{
   final Token t;
   final String curTokenImage;
   final int beginLine;
   final int endLine;
   final int beginColumn;
   final int endColumn;
   String im = jjstrLiteralImages[jjmatchedKind];
   curTokenImage = (im == null) ? input_stream.GetImage() : im;
   beginLine = input_stream.getBeginLine();
   beginColumn = input_stream.getBeginColumn();
   endLine = input_stream.getEndLine();
   endColumn = input_stream.getEndColumn();
   t = Token.newToken(jjmatchedKind, curTokenImage);

   t.beginLine = beginLine;
   t.endLine = endLine;
   t.beginColumn = beginColumn;
   t.endColumn = endColumn;

   return t;
}

int curLexState = 0;
int defaultLexState = 0;
int jjnewStateCnt;
int jjround;
int jjmatchedPos;
int jjmatchedKind;

/** Get the next Token. */
public Token getNextToken() 
{
  Token matchedToken;
  int curPos = 0;

  EOFLoop :
  for (;;)
  {
   try
   {
      curChar = input_stream.BeginToken();
   }
   catch(java.io.IOException e)
   {
      jjmatchedKind = 0;
      matchedToken = jjFillToken();
      return matchedToken;
   }

   try { input_stream.backup(0);
      while (curChar <= 32 && (0x100002200L & (1L << curChar)) != 0L)
         curChar = input_stream.BeginToken();
   }
   catch (java.io.IOException e1) { continue EOFLoop; }
   jjmatchedKind = 0x7fffffff;
   jjmatchedPos = 0;
   curPos = jjMoveStringLiteralDfa0_0();
   if (jjmatchedKind != 0x7fffffff)
   {
      if (jjmatchedPos + 1 < curPos)
         input_stream.backup(curPos - jjmatchedPos - 1);
      if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L)
      {
         matchedToken = jjFillToken();
         return matchedToken;
      }
      else
      {
         continue EOFLoop;
      }
   }
   int error_line = input_stream.getEndLine();
   int error_column = input_stream.getEndColumn();
   String error_after = null;
   boolean EOFSeen = false;
   try { input_stream.readChar(); input_stream.backup(1); }
   catch (java.io.IOException e1) {
      EOFSeen = true;
      error_after = curPos <= 1 ? "" : input_stream.GetImage();
      if (curChar == '\n' || curChar == '\r') {
         error_line++;
         error_column = 0;
      }
      else
         error_column++;
   }
   if (!EOFSeen) {
      input_stream.backup(1);
      error_after = curPos <= 1 ? "" : input_stream.GetImage();
   }
   throw new TokenMgrError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);
  }
}

private void jjCheckNAdd(int state)
{
   if (jjrounds[state] != jjround)
   {
      jjstateSet[jjnewStateCnt++] = state;
      jjrounds[state] = jjround;
   }
}
private void jjAddStates(int start, int end)
{
   do {
      jjstateSet[jjnewStateCnt++] = jjnextStates[start];
   } while (start++ != end);
}
private void jjCheckNAddTwoStates(int state1, int state2)
{
   jjCheckNAdd(state1);
   jjCheckNAdd(state2);
}

private void jjCheckNAddStates(int start, int end)
{
   do {
      jjCheckNAdd(jjnextStates[start]);
   } while (start++ != end);
}

}
