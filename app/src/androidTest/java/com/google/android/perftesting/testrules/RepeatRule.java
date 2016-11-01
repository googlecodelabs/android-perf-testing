package com.google.android.perftesting.testrules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.android.perftesting.common.Repeat;




public class RepeatRule implements TestRule{

    private static class RepeatStatement extends Statement {
        private static int time;
        private final Statement statement;

        private RepeatStatement(int time, Statement statement) {
            this.time = time;
            this.statement = statement;
        }

        @Override
        public void evaluate() throws Throwable {

            Thread threadb = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(time);
                        counter.addNoOfThreadRunnig(-1);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }

                }
            });
            counter.addNoOfThreadRunnig(1);
            threadb.start();


            while(true) {
                if (counter.getNoOfThreadRunnig() == 0)
                    break;
                statement.evaluate();
            }
        }
    }



  @Override
  public Statement apply( Statement statement, Description description ) {
      Statement result = statement;
      Repeat repeat = description.getAnnotation( Repeat.class );
      if( repeat != null ) {
          int time = repeat.time();
          result = new RepeatStatement( time, statement );
      }
      return result;
  }
}
class counter {

    public counter() {
    }
    static int totalNoOfThreadRunnig = 0;
    public static int getNoOfThreadRunnig(){
        return totalNoOfThreadRunnig;
    }
    public static void addNoOfThreadRunnig(int No){
        totalNoOfThreadRunnig += No;
    }

}
