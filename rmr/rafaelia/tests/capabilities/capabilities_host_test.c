/* Copyright (C) 2026 Rafael Melo Reis (RmR) */
/* Hosted adversarial fixture for capacity, lifetime, ABA and concurrent release. */
#include "../rafaelia_capabilities.h"
#include <stdint.h>
#include <stdio.h>
#include <pthread.h>
#define T(c) do { if(!(c)){fprintf(stderr,"FAIL at %d: %s\n",__LINE__,#c);return 1;} ++n;}while(0)
struct args {uint64_t h;int failures;};
static void* worker(void* p){
    struct args* a=(struct args*)p;
    for(int i=0;i<50000;i++){
        int ok=rmr_cap_fill(a->h,0,1,(uint8_t)i);
        if(!ok) break;
    }
    return NULL;
}
int main(void) {
    int n=0;
    T(rmr_cap_allocate(0,64)==0);
    T(rmr_cap_allocate(10,3)==0);
    T(rmr_cap_allocate(10,2)==0);
    T(rmr_cap_allocate(16u*1024u*1024u+1u,64)==0);
    T(rmr_cap_release(0)==0);
    uint64_t a=rmr_cap_allocate(64,64), b=rmr_cap_allocate(64,64);
    T(a && b && a!=b);
    uint8_t v=255;
    T(rmr_cap_read(a,0,&v) && v==0);
    T(rmr_cap_fill(a,0,64,0x11));
    T(rmr_cap_fill(a,63,2,0x00)==0);
    T(rmr_cap_fill(a,SIZE_MAX,1,0)==0);
    T(rmr_cap_copy(a,0,b,0,64));
    T(rmr_cap_read(b,63,&v) && v==0x11);
    T(rmr_cap_copy(a,0,b,1,64)==0);
    T(rmr_cap_copy(b,0,b,8,56));
    T(rmr_cap_read(b,8,&v) && v==0x11);
    T(rmr_cap_write(a,0,0x93));
    T(rmr_cap_read(a,0,&v) && v==0x93);
    T(rmr_cap_write(a,64,0)==0);
    T(rmr_cap_read(a,64,&v)==0);
    T(rmr_cap_fill(a,64,0,0));
    T(rmr_cap_live_count()==2);
    T(rmr_cap_release(a)==1);
    T(rmr_cap_release(a)==0);
    T(rmr_cap_read(a,0,&v)==0);
    uint64_t c=rmr_cap_allocate(64,64);
    T(c!=0 && c!=a);
    T(rmr_cap_copy(a,0,c,0,1)==0);
    T(rmr_cap_release(c));
    T(rmr_cap_release(b));
    T(rmr_cap_live_count()==0);
    uint64_t x=rmr_cap_allocate(1024,64);
    T(x!=0);
    struct args arg={x,0};
    pthread_t t;
    T(pthread_create(&t,0,worker,&arg)==0);
    T(rmr_cap_release(x)==1);
    T(pthread_join(t,0)==0);
    T(rmr_cap_fill(x,0,1,0)==0);
    T(rmr_cap_live_count()==0);
    uint64_t handles[64]={0};
    for(int i=0;i<64;i++){handles[i]=rmr_cap_allocate(1,64);T(handles[i]!=0);}
    T(rmr_cap_allocate(1,64)==0);
    for(int i=0;i<64;i++)T(rmr_cap_release(handles[i]));
    T(rmr_cap_live_count()==0);
    printf("RAFCAP HOST PASS_SCOPED %d checks\n",n);
    return 0;
}
