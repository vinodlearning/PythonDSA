
## get fibo numbers until n

def fib_until_n(n):
    result =[]
    a,b=0,1
    result.append(a)
    while a<=n:
        a,b=b,a+b
        result.append(a)
    return result;

## get nth fibo number

def nth_fib(n):
     ## base case
    if n==0 or n==1:
         return n
    
    return nth_fib(n-1)+nth_fib(n-2)


print(fib_until_n(5))

print(nth_fib(4))

if __name__=="__main__":
    print(f" The {__name__} is loaded successfully -----")