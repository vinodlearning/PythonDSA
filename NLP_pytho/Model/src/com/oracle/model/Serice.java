package com.oracle.model;

import java.util.ArrayList;
import java.util.List;

public class Serice {
    public Serice() {
        super();
    }
    public List<EMP> pull(){
    List<EMP> ss=new ArrayList<EMP>();
    
    for(int i=0;i<20;i++){
        ss.add(new EMP(i));
    }
    return  ss;
    }
    
    public class EMP{
       int id=0;

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        EMP(int i){
            this.id=i;
        }
    }
}
