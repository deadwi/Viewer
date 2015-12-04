#ifndef _SPLIT_H_
#define _SPLIT_H_

#include <vector>
#include <string>

inline void split(const std::string& str,char delimiter,std::vector<std::string>& ret)
{
    typedef std::string::size_type s_size;
    s_size tempi=0,tempj;
    while(tempi!=str.size())
    {
        while(tempi!=str.size() && delimiter==str[tempi])
            tempi++;
        tempj=tempi;

        while(tempj!=str.size() && delimiter!=str[tempj])
            tempj++;

        if(tempi!=tempj)
        {
            ret.push_back(str.substr(tempi,tempj-tempi));
            tempi=tempj;
        }
    }
}

inline void split(const std::string& str,char delimiter,std::vector<unsigned int>& ret)
{
    typedef std::string::size_type s_size;
    s_size tempi=0,tempj;
    while(tempi!=str.size())
    {
        while(tempi!=str.size() && delimiter==str[tempi])
            tempi++;
        tempj=tempi;

        while(tempj!=str.size() && delimiter!=str[tempj])
            tempj++;

        if(tempi!=tempj)
        {
            ret.push_back(atoi(str.substr(tempi,tempj-tempi).c_str()));
            tempi=tempj;
        }
    }
}


#endif // _SPLIT_H_
