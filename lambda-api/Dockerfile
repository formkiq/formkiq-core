FROM public.ecr.aws/lambda/provided

COPY ./runtime/bootstrap /var/runtime/
COPY ./build/graalvm/server /var/task/

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "com.formkiq.stacks.api.CoreRequestHandler" ]  
